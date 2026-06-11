package com.gov.grid.service;

import com.gov.grid.entity.GridMember;

import java.util.List;

public interface GridMemberService {

    void addMember(Long gridId, Long userId, String memberType);

    void removeMember(Long gridId, Long userId);

    void batchAddMembers(Long gridId, List<Long> userIds, String memberType);

    void batchRemoveMembers(Long gridId, List<Long> userIds);

    List<GridMember> getMembersByGridId(Long gridId);

    List<GridMember> getGridsByUserId(Long userId);
}
