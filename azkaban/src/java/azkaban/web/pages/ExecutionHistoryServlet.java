/*
 * Copyright 2010 LinkedIn, Inc
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package azkaban.web.pages;

import azkaban.common.web.Page;
import azkaban.flow.ExecutableFlow;
import azkaban.flow.FlowManager;
import azkaban.flow.Flows;
import azkaban.web.AbstractAzkabanServlet;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExecutionHistoryServlet extends AbstractAzkabanServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
            IOException {
        final FlowManager allFlows = this.getApplication().getAllFlows();

        if (hasParam(req, "action")) {
            if ("restart".equals(getParam(req, "action")) && hasParam(req, "id")) {
                try {
                    long id = Long.parseLong(getParam(req, "id"));

                    final ExecutableFlow flow = allFlows.loadExecutableFlow(id);

                    if (flow == null) {
                        addMessage(req, String.format("Unknown flow with id[%s]", id));
                    }
                    else {
                        Flows.resetFailedFlows(flow);
                        this.getApplication().getScheduler().restartFlow(flow);

                        addMessage(req, String.format("Flow[%s] restarted.", id));
                    }
                }
                catch (NumberFormatException e) {
                    addMessage(req, String.format("Apparently [%s] is not a valid long.", getParam(req, "id")));
                }
            }
        }

        long currMaxId = allFlows.getCurrMaxId();

        int size = 25;
        String sizeParam = req.getParameter("size");
        if(sizeParam != null)
            size = Integer.parseInt(sizeParam);

        List<FlowAndScheduledTime> execs = new ArrayList<FlowAndScheduledTime>(size);
        for (int i = 0; i < size; ++i) {
            ExecutableFlow flow = allFlows.loadExecutableFlow(currMaxId - i);
            
            if (flow != null) {
                String scheduledTimeString = flow.getOverrideProps().get("azkaban.flow.scheduled.timestamp");
                DateTime scheduledTime = null;
                if (scheduledTimeString != null) {
                    scheduledTime = ISODateTimeFormat.dateTime().parseDateTime(scheduledTimeString);
                }
                FlowAndScheduledTime flowAndScheduledTime = new FlowAndScheduledTime(flow, scheduledTime);
                execs.add(flowAndScheduledTime);
            }
        }

        Page page = newPage(req, resp, "azkaban/web/pages/execution_history.vm");
        page.add("executions", execs);
        page.render();
    }

    public class FlowAndScheduledTime {
        private final ExecutableFlow _flow;
        private final DateTime _scheduledTime;

        public FlowAndScheduledTime(ExecutableFlow flow, DateTime scheduledTime) {
            _flow = flow;
            _scheduledTime = scheduledTime;
        }

        public ExecutableFlow getFlow() {
            return _flow;
        }

        public DateTime getScheduledTime() {
            return _scheduledTime;
        }
    }

}

