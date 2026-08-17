package com.chiselon.clinicadmin.service;

import com.chiselon.clinicadmin.dto.FollowOptionDTO;
import com.chiselon.clinicadmin.dto.Response;

public interface FollowOptionService {

    Response create(FollowOptionDTO dto);

    Response getAll();

    Response getById(String id);

    Response update(String id, FollowOptionDTO dto);

    Response delete(String id);
}
