/*******************************************************************************
 * Copyright (c) 2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.eclipse.core.launcher;

import static com.codenvy.client.model.BuilderState.CANCELLED;
import static com.codenvy.client.model.BuilderState.FAILED;
import static com.codenvy.client.model.BuilderState.IN_PROGRESS;
import static com.codenvy.client.model.BuilderState.SUCCESSFUL;
import static com.codenvy.eclipse.core.CodenvyPlugin.PLUGIN_ID;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.eclipse.core.runtime.IStatus.ERROR;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IStreamsProxy;

import com.codenvy.client.Codenvy;
import com.codenvy.client.CodenvyAPI;
import com.codenvy.client.CodenvyErrorException;
import com.codenvy.client.model.BuilderState;
import com.codenvy.client.model.BuilderStatus;
import com.codenvy.client.model.Link;
import com.codenvy.client.model.ProjectReference;
import com.codenvy.eclipse.core.CodenvyPlugin;
import com.codenvy.eclipse.core.CodenvyProjectMetadata;

/**
 * The codenvy builder process.
 * 
 * @author Kevin Pollet
 */
public final class CodenvyBuilderProcess implements IProcess {
    private static final int                TICK_DELAY     = 500;
    private static final TimeUnit           TICK_TIME_UNIT = MILLISECONDS;

    private final ILaunch                   launch;
    private final Codenvy                   codenvy;
    private final ProjectReference          project;
    private final Map<String, String>       attributes;
    private long                            taskId;
    private final ScheduledExecutorService  executorService;
    private final StringBufferStreamMonitor outputStream;
    private final StringBufferStreamMonitor errorStream;
    private int                             exitValue;
    private volatile BuilderState           status;

    /**
     * Constructs an instance of {@link CodenvyBuilderProcess}.
     * 
     * @param launch the {@link ILaunch} object.
     * @param projectMetadata the {@link CodenvyProjectMetadata}.
     * @throws NullPointerException if launch or codenvyMetaProject parameter is {@code null}.
     */
    public CodenvyBuilderProcess(ILaunch launch, CodenvyProjectMetadata projectMetadata) {
        checkNotNull(projectMetadata);

        this.launch = checkNotNull(launch);
        this.project = CodenvyAPI.getClient().newProjectBuilder().withName(projectMetadata.projectName)
                                 .withWorkspaceId(projectMetadata.workspaceId)
                                 .build();

        this.codenvy = CodenvyPlugin.getDefault()
                                    .getCodenvyBuilder(projectMetadata.url, projectMetadata.username)
                                    .build();

        this.attributes = new HashMap<>();
        this.executorService = Executors.newScheduledThreadPool(4);
        this.outputStream = new StringBufferStreamMonitor();
        this.errorStream = new StringBufferStreamMonitor();
        this.exitValue = 0;

        this.attributes.put(ATTR_PROCESS_TYPE, getClass().getName());
        launch.addProcess(this);

        try {

            final BuilderStatus builderStatus = codenvy.builder()
                                                       .build(project)
                                                       .execute();

            this.taskId = builderStatus.taskId();
            this.status = builderStatus.status();

            executorService.scheduleAtFixedRate(new CodenvyBuilderThread(), 0, TICK_DELAY, TICK_TIME_UNIT);

        } catch (CodenvyErrorException e) {
            terminateWithAnError(e);
        }
    }

    @Override
    public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
        if (ILaunch.class.equals(adapter)) {
            return getLaunch();
        }
        return null;
    }

    @Override
    public boolean canTerminate() {
        return !isTerminated();
    }

    @Override
    public boolean isTerminated() {
        return status == CANCELLED || status == SUCCESSFUL || status == FAILED;
    }

    @Override
    public void terminate() throws DebugException {
        try {

            codenvy.builder()
                   .cancel(project, taskId)
                   .execute();

            status = CANCELLED;

            stopProcess();

        } catch (CodenvyErrorException e) {
            terminateWithAnError(e);
        }
    }

    private void terminateWithAnError(CodenvyErrorException exception) {
        errorStream.append("Error: " + exception.getMessage());
        exitValue = exception.getStatus();
        status = FAILED;

        stopProcess();
    }

    private void stopProcess() {
        executorService.shutdownNow();
        fireDebugEvent(DebugEvent.TERMINATE);
    }

    @Override
    public String getLabel() {
        return "Building project on Codenvy";
    }

    @Override
    public ILaunch getLaunch() {
        return launch;
    }

    @Override
    public IStreamsProxy getStreamsProxy() {
        return new IStreamsProxy() {
            @Override
            public void write(String input) throws IOException {
            }

            @Override
            public IStreamMonitor getErrorStreamMonitor() {
                return errorStream;
            }

            @Override
            public IStreamMonitor getOutputStreamMonitor() {
                return outputStream;
            }
        };
    }

    @Override
    public void setAttribute(String key, String value) {
        synchronized (attributes) {
            attributes.put(key, value);
        }

        fireDebugEvent(DebugEvent.CHANGE);
    }

    @Override
    public String getAttribute(String key) {
        synchronized (attributes) {
            return attributes.get(key);
        }
    }

    @Override
    public int getExitValue() throws DebugException {
        if (!isTerminated()) {
            throw new DebugException(new Status(ERROR, PLUGIN_ID, "Process not yet terminated"));
        }
        return exitValue;
    }

    private void fireDebugEvent(int kind) {
        DebugPlugin.getDefault().fireDebugEventSet(new DebugEvent[]{new DebugEvent(this, kind)});
    }

    /**
     * {@link Runnable} polling the builder status and logs.
     * 
     * @author Kevin Pollet
     */
    class CodenvyBuilderThread implements Runnable {
        @Override
        public void run() {
            try {

                final BuilderStatus builderStatus = codenvy.builder()
                                                           .status(project, taskId)
                                                           .execute();

                final Link downloadLink = builderStatus.getDownloadLink();
                boolean isLogsAppended = false;

                status = builderStatus.status();

                if (status == IN_PROGRESS || status == SUCCESSFUL || status == FAILED) {
                    isLogsAppended = appendLogsToOutputStream();
                }

                if (isTerminated() && !isLogsAppended) {
                    if (downloadLink != null) {
                        outputStream.append("\n\nLink to download build result: " + downloadLink.href() + "\n");
                    }

                    stopProcess();
                }

            } catch (CodenvyErrorException e) {
                terminateWithAnError(e);
            }
        }

        /**
         * Appends builder logs to output stream.
         * 
         * @return {@code true} if logs have been appended, {@code false} otherwise.
         */
        private boolean appendLogsToOutputStream() {
            final String fullLogs = codenvy.builder()
                                           .logs(project, taskId)
                                           .execute()
                                           .trim();

            final String logsDiff = fullLogs.substring(outputStream.getContents().length());

            if (!logsDiff.isEmpty()) {
                outputStream.append(logsDiff);
                return true;
            }
            return false;
        }
    }
}
