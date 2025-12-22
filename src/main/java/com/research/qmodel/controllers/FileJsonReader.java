package com.research.qmodel.controllers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public interface FileJsonReader {
    default String readJsonFile(String filePath) {
        try {
            byte[] encoded = Files.readAllBytes(Paths.get(filePath));
            return new String(encoded);
        } catch (IOException e) {
            return "{}";
        }
    }
}
