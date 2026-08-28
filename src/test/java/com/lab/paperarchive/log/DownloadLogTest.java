package com.lab.paperarchive.log;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DownloadLogTest {

    @Test
    void defaultsActionToDownload() {
        DownloadLog log = DownloadLog.builder().build();

        assertThat(log.getAction()).isEqualTo("DOWNLOAD");
    }
}
