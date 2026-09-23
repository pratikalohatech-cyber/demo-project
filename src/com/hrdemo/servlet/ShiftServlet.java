package com.hrdemo.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Minimal REST-ish backend for the Fabric.js + GSAP shift-planning demo
 * (see web/js/shift-planner.js and web/shift-planner.jsp).
 *
 * This is intentionally the "vanilla AJAX" side of the AJAX/REST topic -
 * the frontend calls this with plain XMLHttpRequest/fetch, not through a
 * framework's data layer, mirroring the jQuery/$.ajax() pattern discussed
 * in the interview prep (frontend here uses fetch for brevity, but the
 * request/response contract is the same regardless of client).
 *
 * ConcurrentHashMap stands in for the "safe shared state under concurrent
 * access" concern from the concurrency topic - multiple managers could be
 * dragging shift blocks and saving at the same time.
 */
public class ShiftServlet extends HttpServlet {

    private final Map<String, String> shifts = new ConcurrentHashMap<>();

    @Override
    public void init() {
        // Seed a couple of demo shifts: shiftId -> "employeeId,startHour,durationHours"
        shifts.put("shift-1", "E001,9,4");
        shifts.put("shift-2", "E002,13,3");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json");
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Map.Entry<String, String> entry : shifts.entrySet()) {
            String[] parts = entry.getValue().split(",");
            if (!first) sb.append(",");
            sb.append(String.format(
                    "{\"id\":\"%s\",\"employeeId\":\"%s\",\"startHour\":%s,\"durationHours\":%s}",
                    entry.getKey(), parts[0], parts[1], parts[2]));
            first = false;
        }
        sb.append("]");
        resp.getWriter().write(sb.toString());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Called when a manager finishes dragging a shift block on the canvas -
        // mirrors the "object:modified" -> persist pattern from the Fabric.js topic.
        String id = req.getParameter("id");
        String employeeId = req.getParameter("employeeId");
        String startHour = req.getParameter("startHour");
        String durationHours = req.getParameter("durationHours");

        if (id == null || employeeId == null || startHour == null || durationHours == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\":\"missing required fields\"}");
            return;
        }

        shifts.put(id, employeeId + "," + startHour + "," + durationHours);

        resp.setContentType("application/json");
        resp.getWriter().write("{\"saved\":true,\"id\":\"" + id + "\"}");
    }
}
