package com.example.demo1.controller;

import com.example.demo1.dto.DemoDto;
import com.example.demo1.service.ExportExcel;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/export")
@RequiredArgsConstructor
public class ExportExcelController {


    private final ExportExcel exportExcel;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);;

    @PostMapping("/excel")
    public String export(@RequestParam ("file") MultipartFile[] file)  {

        try {
            List<DemoDto> demoDtos = new ArrayList<>();
            for (MultipartFile f : file) {
                demoDtos.addAll(objectMapper.readValue(f.getInputStream(), new TypeReference<List<DemoDto>>() {}));
            }
            exportExcel.writeExcel2(demoDtos);
            return "success";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
