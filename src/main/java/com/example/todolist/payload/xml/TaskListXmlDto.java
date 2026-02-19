package com.example.todolist.payload.xml;


import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

import java.util.List;

@Data
@JacksonXmlRootElement(localName = "TaskList")
public class TaskListXmlDto {

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "Task")
        private List<TaskXmlDto> tasks;
}
