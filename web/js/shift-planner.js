/**
 * Shift-planning canvas demo.
 *
 * Ties together three interview-prep topics in one working flow:
 *   1. Fabric.js  - object model, groups, object:modified event
 *   2. GSAP       - tweening a Fabric object's properties + canvas.requestRenderAll()
 *   3. jQuery/$.ajax() - vanilla REST call to persist the change, no framework re-render
 *
 * This is a self-contained, imperative script - exactly the "pre-React mental
 * model" discussed in the interview prep: nothing re-renders automatically,
 * every update is explicit.
 */

const PIXELS_PER_HOUR = 60;
const ROW_HEIGHT = 50;
const START_HOUR = 8; // canvas starts at 8am

const canvas = new fabric.Canvas('shiftCanvas', {
    selection: false
});

// Each Fabric object gets a custom `data` property linking the visual shape
// back to its domain entity (the shift ID) - this is how you avoid inferring
// which shift a shape represents purely from its coordinates.
function createShiftBlock(shift) {
    const rect = new fabric.Rect({
        width: shift.durationHours * PIXELS_PER_HOUR,
        height: 40,
        fill: '#4a90d9',
        rx: 6,
        ry: 6,
        originX: 'left',
        originY: 'top'
    });

    const label = new fabric.Text(shift.employeeId, {
        fontSize: 14,
        fill: '#fff',
        left: 8,
        top: 10
    });

    const group = new fabric.Group([rect, label], {
        left: (shift.startHour - START_HOUR) * PIXELS_PER_HOUR,
        top: 20,
        hasControls: false, // demo keeps it simple: draggable, not resizable
    });

    // Custom data linking this Fabric object back to the domain entity.
    group.data = {
        shiftId: shift.id,
        employeeId: shift.employeeId,
        durationHours: shift.durationHours
    };

    return group;
}

function loadShifts() {
    // Vanilla-REST GET via jQuery, not fetch/axios - matches the "AJAX/REST
    // from vanilla JS/jQuery" interview topic. No framework watching this
    // data; the success callback is where we manually build the canvas.
    $.ajax({
        url: '/api/shifts',
        method: 'GET',
        success: function (shifts) {
            canvas.clear();
            shifts.forEach(function (shift) {
                const block = createShiftBlock(shift);
                canvas.add(block);
                attachDragHandling(block);
            });
            setStatus('Loaded ' + shifts.length + ' shifts.');
        },
        error: function (jqXHR) {
            setStatus('Failed to load shifts: ' + jqXHR.status);
        }
    });
}

function attachDragHandling(block) {
    // Fabric event: fires when a drag/resize/rotate finishes - this is the
    // point where we snap to the grid, animate with GSAP, and persist via AJAX.
    block.on('modified', function () {
        const snappedLeft = Math.round(block.left / PIXELS_PER_HOUR) * PIXELS_PER_HOUR;
        const snappedTop = Math.max(0, Math.round(block.top / ROW_HEIGHT) * ROW_HEIGHT);

        // GSAP tween animates the Fabric object's own properties (not DOM/CSS).
        // Because Fabric objects aren't DOM elements, the browser won't repaint
        // automatically - onUpdate calls canvas.requestRenderAll() every frame
        // so the animation is actually visible.
        gsap.to(block, {
            left: snappedLeft,
            top: snappedTop,
            duration: 0.3,
            ease: 'power2.out',
            onUpdate: function () {
                canvas.requestRenderAll();
            },
            onComplete: function () {
                const newStartHour = START_HOUR + (snappedLeft / PIXELS_PER_HOUR);
                saveShiftPosition(block.data.shiftId, block.data.employeeId,
                        newStartHour, block.data.durationHours);
            }
        });
    });
}

function saveShiftPosition(shiftId, employeeId, startHour, durationHours) {
    // Vanilla-REST POST via jQuery - manual DOM/status update in the success
    // callback, no framework state triggering it automatically.
    $.ajax({
        url: '/api/shifts',
        method: 'POST',
        data: {
            id: shiftId,
            employeeId: employeeId,
            startHour: startHour,
            durationHours: durationHours
        },
        success: function (response) {
            setStatus('Saved ' + shiftId + ' at hour ' + startHour + ' (server ack: ' + response.saved + ')');
        },
        error: function (jqXHR) {
            setStatus('Failed to save ' + shiftId + ': ' + jqXHR.status);
        }
    });
}

function setStatus(message) {
    // Direct DOM manipulation - no virtual DOM, no React re-render.
    // We own finding the element and updating it, every time.
    document.getElementById('status').textContent = message;
}

// Kick off on page load.
loadShifts();
