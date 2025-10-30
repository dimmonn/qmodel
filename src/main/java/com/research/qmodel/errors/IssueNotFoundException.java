package com.research.qmodel.errors;

public class IssueNotFoundException extends RuntimeException{

  public IssueNotFoundException(String message) {
    super(message);
  }
}
