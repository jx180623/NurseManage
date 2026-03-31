package com.xnl.qc.service.impl;

import com.xnl.qc.dto.Dto.*;
import com.xnl.qc.entity.Nurse;
import com.xnl.qc.exception.BusinessException;
import com.xnl.qc.repository.NurseRepository;
import com.xnl.qc.service.NurseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NurseServiceImpl implements NurseService {

    private static final String DEFAULT_PW = "XNL226";

    private final NurseRepository nurseRepository;

    @Override
    @Transactional(readOnly = true)
    public List<NurseDto> listEnabled() {
        return nurseRepository.findByEnabledTrueOrderByEmployeeId()
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<NurseDto> listAll() {
        return nurseRepository.findAll()
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public NurseDto create(NurseCreateRequest req) {
        String empId = req.getEmployeeId().trim().toUpperCase();
        if (nurseRepository.existsByEmployeeIdIgnoreCase(empId)) {
            throw new BusinessException("工号 [" + empId + "] 已存在");
        }
        Nurse nurse = new Nurse();
        nurse.setEmployeeId(empId);
        nurse.setName(req.getName().trim());
        nurse.setPassword(StringUtils.hasText(req.getPassword())
                ? req.getPassword() : DEFAULT_PW);
        nurse.setEnabled(true);
        log.info("新增护士 - 工号: {}", empId);
        return toDto(nurseRepository.save(nurse));
    }

    @Override
    @Transactional
    public NurseDto update(Long id, NurseUpdateRequest req) {
        Nurse nurse = nurseRepository.findById(id)
                .orElseThrow(() -> new BusinessException("护士（ID=" + id + "）不存在"));
        String newId = req.getEmployeeId().trim().toUpperCase();
        if (!nurse.getEmployeeId().equalsIgnoreCase(newId)
                && nurseRepository.existsByEmployeeIdIgnoreCase(newId)) {
            throw new BusinessException("工号 [" + newId + "] 已被其他护士占用");
        }
        nurse.setEmployeeId(newId);
        nurse.setName(req.getName().trim());
        if (req.getEnabled() != null) nurse.setEnabled(req.getEnabled());
        log.info("修改护士信息 - ID: {}, 新工号: {}", id, newId);
        return toDto(nurseRepository.save(nurse));
    }

    @Override
    @Transactional
    public void resetPassword(ResetNursePwRequest req) {
        Nurse nurse = nurseRepository.findByEmployeeIdIgnoreCase(req.getEmployeeId())
                .orElseThrow(() -> new BusinessException("护士工号 [" + req.getEmployeeId() + "] 不存在"));
        String newPw = StringUtils.hasText(req.getNewPassword())
                && req.getNewPassword().length() >= 4
                ? req.getNewPassword() : DEFAULT_PW;
        nurse.setPassword(newPw);
        nurseRepository.save(nurse);
        log.info("管理员重置密码 - 工号: {}, 是否默认密码: {}", req.getEmployeeId(), DEFAULT_PW.equals(newPw));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Nurse nurse = nurseRepository.findById(id)
                .orElseThrow(() -> new BusinessException("护士（ID=" + id + "）不存在"));
        nurseRepository.delete(nurse);
        log.info("删除护士 - 工号: {}", nurse.getEmployeeId());
    }

    private NurseDto toDto(Nurse n) {
        NurseDto dto = new NurseDto();
        dto.setId(n.getId());
        dto.setEmployeeId(n.getEmployeeId());
        dto.setName(n.getName());
        dto.setEnabled(n.getEnabled());
        // 有个人密码且不等于默认密码 → hasCustomPassword=true
        dto.setHasCustomPassword(StringUtils.hasText(n.getPassword())
                && !DEFAULT_PW.equals(n.getPassword()));
        return dto;
    }
}
