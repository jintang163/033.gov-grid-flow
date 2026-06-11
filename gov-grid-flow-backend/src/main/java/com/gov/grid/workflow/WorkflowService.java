package com.gov.grid.workflow;

import com.gov.grid.entity.EventInfo;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.task.api.Task;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface WorkflowService {

    String startProcess(EventInfo eventInfo);

    String startProcess(String eventId, Map<String, Object> variables);

    void completeTask(String taskId, String userId, Map<String, Object> variables);

    List<Task> getTaskList(String userId, String processInstanceId);

    Task getTaskById(String taskId);

    void assignTask(String taskId, String userId);

    InputStream getProcessDiagram(String processInstanceId);

    List<HistoricActivityInstance> getHistoryActivityList(String processInstanceId);

    void rejectTask(String taskId, String userId, String comment);
}
