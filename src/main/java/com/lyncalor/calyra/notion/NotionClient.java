package com.lyncalor.calyra.notion;

public interface NotionClient {

    java.util.Optional<String> createScheduleEntry(com.lyncalor.calyra.schedule.ScheduleDraft draft,
                                                   String rawText,
                                                   String source);

    boolean createSimpleEntry(String title, String rawText, String source);
}
