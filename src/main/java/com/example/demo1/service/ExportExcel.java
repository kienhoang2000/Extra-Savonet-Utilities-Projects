package com.example.demo1.service;

import com.example.demo1.dto.DemoDto;

import java.io.ByteArrayInputStream;
import java.util.List;

public interface ExportExcel {
    ByteArrayInputStream writeExcel(List<DemoDto> demoDtos);

    void writeExcel2(List<DemoDto> demoDtos);
}
