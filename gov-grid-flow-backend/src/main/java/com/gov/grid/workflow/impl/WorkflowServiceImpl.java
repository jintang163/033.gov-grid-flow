package com.gov.grid.workflow.impl;

import com.gov.grid.common.exception.BusinessException;
import com.gov.grid.entity.EventInfo;
import com.gov.grid.workflow.WorkflowService;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.task.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(rollbackFor = Exception.class)
public class WorkflowServiceImpl implements WorkflowService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowServiceImpl.class);

    private static final String PROCESS_DEFINITION_KEY = "eventDisposal";

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private ManagementService managementService;

    @Override
    public String startProcess(EventInfo eventInfo) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("eventId", String.valueOf(eventInfo.getId()));
        variables.put("eventNo", eventInfo.getEventNo());
        variables.put("gridId", eventInfo.getGridId());
        variables.put("reporterId", eventInfo.getReporterId());
        variables.put("priority", eventInfo.getPriority());
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                PROCESS_DEFINITION_KEY, String.valueOf(eventInfo.getId()), variables);
        log.info("流程启动成功，事件ID: {}, 事件编号: {}, 流程实例ID: {}",
                eventInfo.getId(), eventInfo.getEventNo(), processInstance.getId());
        return processInstance.getId();
    }

    @Override
    public String startProcess(String eventId, Map<String, Object> variables) {
        if (variables == null) {
            variables = new HashMap<>();
        }
        variables.put("eventId", eventId);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                PROCESS_DEFINITION_KEY, eventId, variables);
        log.info("流程启动成功，事件ID: {}, 流程实例ID: {}", eventId, processInstance.getId());
        return processInstance.getId();
    }

    @Override
    public void completeTask(String taskId, String userId, Map<String, Object> variables) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new BusinessException("任务不存在，任务ID: " + taskId);
        }
        if (variables == null) {
            variables = new HashMap<>();
        }
        variables.putIfAbsent("approved", true);
        variables.putIfAbsent("handleCompleted", true);
        variables.putIfAbsent("verifyPassed", true);
        variables.put("reject", false);
        taskService.addComment(taskId, task.getProcessInstanceId(), "USER_ACTION",
                "用户[" + userId + "]完成任务: " + task.getName());
        taskService.complete(taskId, variables);
        log.info("任务完成，任务ID: {}, 用户ID: {}", taskId, userId);
    }

    @Override
    public List<Task> getTaskList(String userId, String processInstanceId) {
        if (processInstanceId != null && !processInstanceId.isEmpty()) {
            return taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .taskCandidateOrAssigned(userId)
                    .orderByTaskCreateTime()
                    .desc()
                    .list();
        }
        return taskService.createTaskQuery()
                .taskCandidateOrAssigned(userId)
                .orderByTaskCreateTime()
                .desc()
                .list();
    }

    @Override
    public Task getTaskById(String taskId) {
        return taskService.createTaskQuery().taskId(taskId).singleResult();
    }

    @Override
    public void assignTask(String taskId, String userId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new BusinessException("任务不存在，任务ID: " + taskId);
        }
        taskService.setAssignee(taskId, userId);
        taskService.addComment(taskId, task.getProcessInstanceId(), "ASSIGN_ACTION",
                "任务分派给用户: " + userId);
        log.info("任务分派成功，任务ID: {}, 分派给用户: {}", taskId, userId);
    }

    @Override
    public InputStream getProcessDiagram(String processInstanceId) {
        HistoricProcessInstance historicProcessInstance = historyService
                .createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (historicProcessInstance == null) {
            throw new BusinessException("流程实例不存在，ID: " + processInstanceId);
        }
        BpmnModel bpmnModel = repositoryService.getBpmnModel(
                historicProcessInstance.getProcessDefinitionId());
        List<String> highLightedActivities = new ArrayList<>();
        List<HistoricActivityInstance> historicActivityInstances = historyService
                .createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .finished()
                .list();
        for (HistoricActivityInstance hai : historicActivityInstances) {
            highLightedActivities.add(hai.getActivityId());
        }
        List<String> runningActivityIds = new ArrayList<>();
        List<Task> runningTasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .list();
        for (Task task : runningTasks) {
            runningActivityIds.add(task.getTaskDefinitionKey());
        }
        ProcessDiagramGenerator diagramGenerator = managementService.getProcessEngineConfiguration()
                .getProcessDiagramGenerator();
        return diagramGenerator.generateDiagram(
                bpmnModel,
                "png",
                highLightedActivities,
                runningActivityIds,
                managementService.getProcessEngineConfiguration().getActivityFontName(),
                managementService.getProcessEngineConfiguration().getLabelFontName(),
                managementService.getProcessEngineConfiguration().getAnnotationFontName(),
                managementService.getProcessEngineConfiguration().getClassLoader(),
                1.0,
                true);
    }

    @Override
    public List<HistoricActivityInstance> getHistoryActivityList(String processInstanceId) {
        return historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime()
                .asc()
                .list();
    }

    @Override
    public void rejectTask(String taskId, String userId, String comment) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new BusinessException("任务不存在，任务ID: " + taskId);
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("reject", true);
        variables.put("approved", false);
        variables.put("verifyPassed", false);
        String commentText = "用户[" + userId + "]退回任务: " + task.getName()
                + (comment != null && !comment.isEmpty() ? "，原因: " + comment : "");
        taskService.addComment(taskId, task.getProcessInstanceId(), "REJECT_ACTION", commentText);
        taskService.complete(taskId, variables);
        log.info("任务退回，任务ID: {}, 用户ID: {}, 原因: {}", taskId, userId, comment);
    }
}
