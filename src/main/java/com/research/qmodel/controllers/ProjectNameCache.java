package com.research.qmodel.controllers;

import static org.springframework.context.annotation.ScopedProxyMode.TARGET_CLASS;

import lombok.Data;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Data
@Component
@Scope(value = "request", proxyMode = TARGET_CLASS)
public class ProjectNameCache {
  private String projectName;
  private String projectOwner;
}
