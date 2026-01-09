package com.ninja.terminal.util;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.jetbrains.jediterm.terminal.Questioner;
import org.jetbrains.jediterm.terminal.TtyConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class JSchTtyConnector implements TtyConnector {
    private static final Logger log = LoggerFactory.getLogger(JSchTtyConnector.class);

    private final ChannelShell channel;
    private final Session session;
    private InputStream inputStream;
    private InputStreamReader inputStreamReader;
    private OutputStream outputStream;

    public JSchTtyConnector(Session session, ChannelShell channel) {
        this.session = session;
        this.channel = channel;
        try {
            this.inputStream = channel.getInputStream();
            this.outputStream = channel.getOutputStream();
            this.inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Error creating streams", e);
        }
    }

    @Override
    public boolean init(Questioner questioner) {
        try {
            channel.connect(3000);
            return true;
        } catch (JSchException e) {
            log.error("Failed to connect channel", e);
            return false;
        }
    }

    @Override
    public void close() {
        if (channel != null) channel.disconnect();
        if (session != null) session.disconnect();
    }

    @Override
    public String getName() {
        return "JSch-SSH";
    }

    @Override
    public int read(char[] buf, int offset, int length) throws IOException {
        return inputStreamReader.read(buf, offset, length);
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        outputStream.write(bytes);
        outputStream.flush();
    }

    @Override
    public void write(String string) throws IOException {
        write(string.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean isConnected() {
        return channel != null && channel.isConnected();
    }

    @Override
    public void resize(Dimension termSize, Dimension pixelSize) {
        if (channel != null && channel.isConnected()) {
            channel.setPtySize(termSize.width, termSize.height, pixelSize.width, pixelSize.height);
        }
    }

    @Override
    public int waitFor() throws InterruptedException {
        while (isConnected()) {
            Thread.sleep(100);
        }
        return channel.getExitStatus();
    }

    @Override
    public boolean ready() throws IOException {
        return inputStreamReader.ready();
    }
}
