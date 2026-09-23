<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <title>Shift Planner</title>
    <link rel="stylesheet" href="css/style.css">
    <!-- Loaded from CDN for this demo; in the real project these were bundled -->
    <script src="https://cdnjs.cloudflare.com/ajax/libs/fabric.js/5.3.0/fabric.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/gsap/3.12.2/gsap.min.js"></script>
    <script src="https://code.jquery.com/jquery-3.7.1.min.js"></script>
</head>
<body>
    <h1>Shift Planner</h1>
    <p>Drag a shift block to a new position. On drop, GSAP animates it settling into its
       snapped slot, then a plain <code>$.ajax()</code> call (jQuery, vanilla-style REST -
       not fetch/axios) saves the new position to <code>/api/shifts</code>.</p>

    <canvas id="shiftCanvas" width="900" height="300" style="border:1px solid #ccc;"></canvas>

    <div id="status" style="margin-top:10px; font-family: monospace;"></div>

    <script src="js/shift-planner.js"></script>
</body>
</html>
