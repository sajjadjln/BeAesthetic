/* Character candidates for the goal-app direction: the hoopoe «هدهد» and the old magician «پیر».
   Same sticker construction as «پیکر» (D81–D82): every solid shape is drawn four times —
   drop shadow, white die-cut border, ink outline, color — so each pose reads as one sticker.
   Detail shapes (eyes, stripes, stars) are drawn only in the color pass.
   Usage: Candidates.svg("hoopoe" | "magician", "wave", "#34C47A") → "<svg …>". */
(function () {
  "use strict";
  var INK = "#1C1715", BORDER = 5, OUTLINE = 2.1, uid = 0;
  function n(v) { return Math.round(v * 100) / 100; }

  /* ---------- parts: {t, a, f, w (line width), tf, ln (ink edge), d (detail), clip} ---------- */
  function mk(t, a, f, o) { var p = { t: t, a: a, f: f }; for (var k in o) p[k] = o[k]; return p; }
  function P(d, f, o) { return mk("path", { d: d }, f, o); }
  function C(cx, cy, r, f, o) { return mk("circle", { cx: cx, cy: cy, r: r }, f, o); }
  function E(cx, cy, rx, ry, f, o) { return mk("ellipse", { cx: cx, cy: cy, rx: rx, ry: ry }, f, o); }
  function R(x, y, w, h, rx, f, o) { return mk("rect", { x: x, y: y, width: w, height: h, rx: rx }, f, o); }
  function L(d, f, w, o) { var p = mk("line", { d: d }, f, o); p.w = w; return p; }

  function tag(p, fill, stroke, sw) {
    var a = "";
    for (var k in p.a) a += " " + k + '="' + (typeof p.a[k] === "number" ? n(p.a[k]) : p.a[k]) + '"';
    var tf = p.tf ? ' transform="' + p.tf + '"' : "", cp = p.clip ? ' clip-path="url(#' + p.clip + ')"' : "";
    var op = p.op ? ' opacity="' + p.op + '"' : "";
    if (p.t === "line") return '<path' + a + ' fill="none" stroke="' + fill + '" stroke-width="' + n(sw) + '" stroke-linecap="round" stroke-linejoin="round"' + tf + cp + op + "/>";
    var st = sw ? ' stroke="' + stroke + '" stroke-width="' + n(sw) + '" stroke-linejoin="round"' : "";
    return "<" + p.t + a + ' fill="' + fill + '"' + st + tf + cp + op + "/>";
  }
  function render(ps) {
    var solid = ps.parts.filter(function (p) { return !p.d; });
    function pass(color, extra) { return solid.map(function (p) { return tag(p, color, color, (p.w || 0) + 2 * extra); }).join(""); }
    var col = ps.parts.map(function (p) { return p.t === "line" ? tag(p, p.f, null, p.w) : tag(p, p.f, INK, p.ln ? 1.6 : 0); }).join("");
    return "<defs>" + ps.defs + '</defs><g transform="translate(1.6 3.2)" opacity=".38">' + pass("#000", BORDER) + "</g>" +
      pass("#FFFFFF", BORDER) + pass(INK, OUTLINE) + col;
  }

  /* ---------- props (shared) ---------- */
  function star(x, y, r, f) { return P("M" + x + " " + (y - r) + "Q" + x + " " + y + " " + (x + r) + " " + y + "Q" + x + " " + y + " " + x + " " + (y + r) + "Q" + x + " " + y + " " + (x - r) + " " + y + "Q" + x + " " + y + " " + x + " " + (y - r) + "Z", f || "#FFD45C", { d: 1 }); }
  function zz(A, x, y) {
    [[x, y, 6], [x + 8, y - 9, 4.4]].forEach(function (z) { A.push(L("M" + z[0] + " " + z[1] + "h" + z[2] + "l" + (-z[2]) + " " + z[2] + "h" + z[2], "#AEB6F5", 2.3, { d: 1 })); });
  }
  function dots(A, pts) { pts.forEach(function (q) { A.push(C(q[0], q[1], q[2], "#E8ECEA", { d: 1 })); }); }
  function cup(A, x, y) {
    A.push(L("M" + (x + 5.6) + " " + (y - 3) + "q4.6 0 3.8 4.2q-.8 3 -4.8 2.6", "#F4EEE6", 2.2));
    A.push(P("M" + (x - 6.2) + " " + (y - 5.4) + "h12.4l-1.4 10q-.4 2.2 -2.6 2.2h-4.4q-2.2 0 -2.6 -2.2Z", "#F4EEE6", { ln: 1 }));
    A.push(E(x, y - 5.2, 5.8, 1.5, "#B4582F", { d: 1 }));
    A.push(L("M" + (x - 2) + " " + (y - 9) + "q-2.2 -3 0 -5.6q2 -2.8 0 -5.8", "#DDE3E0", 1.5, { d: 1 }));
    A.push(L("M" + (x + 2.6) + " " + (y - 10) + "q-2 -2.6 0 -5q1.8 -2.4 0 -5", "#DDE3E0", 1.5, { d: 1 }));
  }
  function shade(hex, t) {
    var v = parseInt(hex.slice(1), 16), r = v >> 16, g = (v >> 8) & 255, b = v & 255;
    function m(c) { return Math.round(c * (1 - t)); }
    return "#" + ((1 << 24) | (m(r) << 16) | (m(g) << 8) | m(b)).toString(16).slice(1);
  }

  /* ================= the hoopoe «هدهد» ================= */
  var HC = { body: "#E9A46E", belly: "#F7D6AE", dark: "#241D1B", bar: "#FFF4E4", beak: "#3B2F2B", leg: "#7E635A", blush: "#F0857A" };
  var WING = "M-3 -8C8 -10.5 22 -7.5 31 0C22 6.5 8 9.5 -3 7.5C-7.5 4 -7.5 -4.5 -3 -8Z";
  var FEATHER = "M0 -3.2C8 -5 17 -5.2 22.5 -2.3Q24.6 0 22.5 2.3C17 5.2 8 5 0 3.2Z";
  function wing(ps, x, y, ang) {
    var id = "cw" + (++uid), tf = "translate(" + x + " " + y + ") rotate(" + ang + ")";
    ps.defs += '<clipPath id="' + id + '"><path d="' + WING + '"/></clipPath>';
    ps.parts.push(P(WING, HC.dark, { tf: tf, ln: 1 }));
    ps.parts.push(R(-9, -12, 10, 24, 0, HC.body, { tf: tf, clip: id, d: 1 }));
    [9, 15.6, 22.2].forEach(function (sx) { ps.parts.push(R(sx, -12, 3.2, 24, 0, HC.bar, { tf: tf, clip: id, d: 1 })); });
  }
  function feather(ps, x, y, ang, len) {
    var tf = "translate(" + x + " " + y + ") rotate(" + ang + ") scale(" + n(len / 24) + ")";
    ps.parts.push(P(FEATHER, HC.dark, { tf: tf, ln: 1 }));
    ps.parts.push(P(FEATHER, HC.bar, { tf: tf + " scale(.8 .86)", d: 1 }));
    ps.parts.push(P(FEATHER, HC.body, { tf: tf + " scale(.67 .86)", d: 1 }));
  }
  var CREST = {
    up: [[-160, 25], [-131, 28], [-103, 29], [-75, 28], [-47, 25]],
    mid: [[-126, 19], [-106, 21], [-86, 21], [-66, 20], [-46, 18]],
    down: [[2, 18], [-7, 21], [-16, 22], [-25, 22], [-34, 20]]
  };
  function hoopoe(o, acc) {
    var ps = { parts: [], defs: "" }, A = ps.parts;
    if (o.wingB != null) wing(ps, 72, 64, o.wingB);
    var ttf = "translate(80 85) rotate(-28)";
    A.push(R(-2, -6.5, 29, 13, 6.5, HC.dark, { tf: ttf }));
    A.push(R(13, -6.5, 5, 13, 0, HC.bar, { tf: ttf, d: 1 }));
    A.push(L("M58 94L56.5 105", HC.leg, 3.6)); A.push(L("M68 95L69.5 105", HC.leg, 3.6));
    A.push(E(53.5, 106, 4.2, 2.1, HC.leg)); A.push(E(72.5, 106, 4.2, 2.1, HC.leg));
    CREST[o.crest || "mid"].forEach(function (f) { feather(ps, 53, 33, f[0], f[1]); });
    A.push(E(64, 77, 23, 20, HC.body));
    A.push(E(56, 82, 13, 12, HC.belly, { d: 1 }));
    A.push(C(49, 46, 18, HC.body));
    // the scarf carries the accent; the bird keeps its own colors
    A.push(P("M40.5 58.5Q55 67 69.5 55L71.5 61.5Q56 74 42 65.5Z", acc, { ln: 1 }));
    A.push(P("M45 64L40.5 75.5L47 76.5L50.5 66Z", acc, { ln: 1 }));
    if (o.wingF != null) wing(ps, o.wx || 64, o.wy || 70, o.wingF);
    if (o.beak === "open") {
      A.push(P("M35 49.5L15.5 60.5L36 55Z", "#5A2422"));
      A.push(P("M35 45.5Q22 47.5 10.5 57Q9.6 59 11.8 59Q24 53 35.5 50.5Z", HC.beak, { ln: 1 }));
      A.push(P("M35.5 51.5Q25 56.5 15 63Q14 64.8 16 64.6Q26 60.5 36 55Z", HC.beak, { ln: 1 }));
    } else {
      A.push(P("M35 45.5Q22 48.5 10.5 59.5Q9.5 61.5 11.8 61.6Q24 55.5 35.5 53.5Z", HC.beak, { ln: 1 }));
    }
    var ex = 41 + (o.ex || 0), ey = 42.5 + (o.ey || 0);
    eye(A, ex, ey, o.eye || "open", 3.5);
    A.push(E(45.5, 51.5, 3.4, 2, HC.blush, { d: 1, op: .6 }));
    prop(A, o);
    return ps;
  }
  function eye(A, x, y, kind, r) {
    if (kind === "happy") A.push(L("M" + (x - 3.8) + " " + (y + 1.5) + "Q" + x + " " + (y - 3.7) + " " + (x + 3.8) + " " + (y + 1.5), INK, 2.1, { d: 1 }));
    else if (kind === "closed") A.push(L("M" + (x - 3.8) + " " + (y - .5) + "Q" + x + " " + (y + 3.9) + " " + (x + 3.8) + " " + (y - .5), INK, 2.1, { d: 1 }));
    else {
      var rr = kind === "wide" ? r * 1.22 : r;
      A.push(C(x, y, rr, INK, { d: 1 }));
      A.push(C(x + rr * .37, y - rr * .37, rr * .35, "#FFFFFF", { d: 1 }));
    }
  }
  function prop(A, o) {
    (o.spark || []).forEach(function (s) { A.push(star(s[0], s[1], s[2])); });
    if (o.zz) zz(A, o.zz[0], o.zz[1]);
    if (o.dots) dots(A, o.dots);
    if (o.cup) cup(A, o.cup[0], o.cup[1]);
  }

  /* ================= the old magician «پیر» ================= */
  var MC = { skin: "#F2C4A0", beard: "#F5F3EE", wisp: "#CFC8BC", nose: "#E99A84", gold: "#F5C451", wood: "#9C6A3E", shoe: "#5A3A2A", blush: "#EE8E80" };
  var HAT = {
    up: "M44.5 37.5C49 28 53 18 57 10C59.5 5 64 1.5 70 2.5C66.5 5 64.5 9 64.2 14C66 22 70.5 30 75.5 37.5Z",
    droop: "M44.5 37.5C49 28 53 20 57.5 14C61 9 68 8 74 13C76 15 77 18 75 20C71 16 67 16 64.5 19C67 25 71 31 75.5 37.5Z"
  };
  var HAT_TIP = { up: [70, 2.5], droop: [75.6, 19.6] };
  var SL = [49, 64], SR = [71, 64];
  function arm(A, S, H, hand, acc) {
    A.push(L("M" + S[0] + " " + S[1] + "L" + H[0] + " " + H[1], acc, 9.5));
    var dx = H[0] - S[0], dy = H[1] - S[1], len = Math.hypot(dx, dy) || 1, fx = dx / len, fy = dy / len;
    A.push(C(S[0] + dx * .74, S[1] + dy * .74, 5.8, acc, { ln: 1 }));
    if (hand === "none") return;
    A.push(C(H[0], H[1], 4.3, MC.skin, { ln: 1 }));
    if (hand === "open") [-42, 0, 42].forEach(function (g) {
      var a = g * Math.PI / 180, vx = fx * Math.cos(a) - fy * Math.sin(a), vy = fx * Math.sin(a) + fy * Math.cos(a);
      A.push(C(H[0] + vx * 4.6, H[1] + vy * 4.6, 1.9, MC.skin, { ln: 1 }));
    });
    if (hand === "thumb") A.push(L("M" + H[0] + " " + (H[1] - 2) + "L" + (H[0] + .6) + " " + (H[1] - 8.6), MC.skin, 3.6));
  }
  function hand(A, x, y) { A.push(C(x, y, 4.3, MC.skin, { ln: 1 })); }
  function staff(A, x1, y1, x2, y2, glow) {
    A.push(L("M" + x1 + " " + y1 + "L" + x2 + " " + y2, MC.wood, 3.2));
    A.push(C(x1, y1, 5.2, MC.gold, { ln: 1 }));
    A.push(C(x1 - 1.6, y1 - 1.6, 1.5, "#FFF6D8", { d: 1 }));
    if (glow) [[x1 - 9, y1 - 4, 3.4], [x1 + 8, y1 - 7, 2.6], [x1 + 3, y1 + 9, 2.2]].forEach(function (s) { A.push(star(s[0], s[1], s[2])); });
  }
  function magician(o, acc) {
    var ps = { parts: [], defs: "" }, A = ps.parts, brim = shade(acc, .25);
    if (o.lean) staff(A, 90, 34, 84, 104, false);
    A.push(E(51, 104, 6.5, 3.3, MC.shoe)); A.push(E(69, 104, 6.5, 3.3, MC.shoe));
    A.push(C(44.6, 102.4, 1.9, MC.shoe)); A.push(C(75.4, 102.4, 1.9, MC.shoe));
    A.push(P("M45 60Q40 82 33 101Q60 110 87 101Q80 82 75 60Q60 54 45 60Z", acc));
    A.push(L("M35.5 98.3Q60 106.6 84.5 98.3", MC.gold, 2.2, { d: 1 }));
    if (o.staff) staff(A, o.staff[0], o.staff[1], o.staff[2], o.staff[3], o.glow);
    (o.back || []).forEach(function (a) { arm(A, a[0], a[1], a[2], acc); });
    A.push(C(46.6, 49.5, 3, MC.skin)); A.push(C(73.4, 49.5, 3, MC.skin));
    A.push(C(60, 47, 13.5, MC.skin));
    A.push(C(46.4, 44.8, 4.6, MC.beard, { ln: 1 })); A.push(C(73.6, 44.8, 4.6, MC.beard, { ln: 1 }));
    var hat = o.hat || "up", tip = HAT_TIP[hat];
    A.push(P(HAT[hat], acc, { ln: 1 }));
    A.push(star(hat === "up" ? 58.5 : 60, hat === "up" ? 24 : 26, 3.4, MC.gold));
    A.push(C(hat === "up" ? 64 : 66, hat === "up" ? 31.5 : 32, 1.3, MC.gold, { d: 1 }));
    A.push(P("M45.4 34.4Q60 38.6 74.6 34.4L75.4 37Q60 41.8 44.6 37Z", MC.gold, { d: 1 }));
    A.push(E(60, 38, 21.5, 5.2, brim, { ln: 1 }));
    A.push(C(tip[0], tip[1], 2.6, MC.gold, { ln: 1 }));
    var by = o.brow === "up" ? 43.6 : 44.8;
    A.push(E(54, by, 4.6, 2.1, MC.beard, { ln: 1, tf: "rotate(-8 54 " + by + ")" }));
    A.push(E(66, by, 4.6, 2.1, MC.beard, { ln: 1, tf: "rotate(8 66 " + by + ")" }));
    var ex = o.ex || 0, ey = o.ey || 0, ek = o.eye || "open";
    meye(A, 54.5 + ex, 50 + ey, ek === "wink" ? "open" : ek);
    meye(A, 65.5 + ex, 50 + ey, ek === "wink" ? "happy" : ek);
    A.push(E(50.6, 54.2, 2.8, 1.7, MC.blush, { d: 1, op: .55 })); A.push(E(69.4, 54.2, 2.8, 1.7, MC.blush, { d: 1, op: .55 }));
    A.push(P("M45.5 52Q41 64 47 76Q52 88 60 95.5Q68 88 73 76Q79 64 74.5 52Q70 59.5 60 60Q50 59.5 45.5 52Z", MC.beard, { ln: 1 }));
    A.push(L("M53.5 70Q55.5 80 59.5 87", MC.wisp, 1.4, { d: 1 })); A.push(L("M66.5 70Q64.5 80 60.5 87", MC.wisp, 1.4, { d: 1 }));
    A.push(L("M48.5 64Q49.5 71 53 76", MC.wisp, 1.4, { d: 1 })); A.push(L("M71.5 64Q70.5 71 67 76", MC.wisp, 1.4, { d: 1 }));
    if (o.mouth === "open") A.push(E(60, 63.6, 2.7, 2.4, "#6A2B2B", { d: 1 }));
    else A.push(L("M57 62.6Q60 65 63 62.6", INK, 1.6, { d: 1 }));
    A.push(E(55, 58.6, 5.6, 2.9, MC.beard, { ln: 1, tf: "rotate(14 55 58.6)" }));
    A.push(E(65, 58.6, 5.6, 2.9, MC.beard, { ln: 1, tf: "rotate(-14 65 58.6)" }));
    A.push(C(60, 55, 3.5, MC.nose, { ln: 1 }));
    (o.front || []).forEach(function (a) { arm(A, a[0], a[1], a[2], acc); });
    if (o.cup) { cup(A, o.cup[0], o.cup[1]); hand(A, o.cup[0] - 6.4, o.cup[1] + 1); hand(A, o.cup[0] + 6.2, o.cup[1] + 1); }
    if (o.fold) { hand(A, 57, 87); hand(A, 63.4, 87.6); }
    prop(A, o);
    return ps;
  }
  function meye(A, x, y, kind) {
    if (kind === "happy") A.push(L("M" + (x - 2.5) + " " + (y + .8) + "Q" + x + " " + (y - 2.4) + " " + (x + 2.5) + " " + (y + .8), INK, 1.7, { d: 1 }));
    else if (kind === "closed") A.push(L("M" + (x - 2.5) + " " + (y - .6) + "Q" + x + " " + (y + 2.2) + " " + (x + 2.5) + " " + (y - .6), INK, 1.7, { d: 1 }));
    else {
      var r = kind === "wide" ? 2.5 : 2.15;
      A.push(C(x, y, r, INK, { d: 1 }));
      A.push(C(x + .7, y - .7, r * .36, "#FFFFFF", { d: 1 }));
    }
  }

  /* ---------- poses: the same six moments for both ---------- */
  var POSES = {
    hoopoe: {
      wave:  { crest: "mid", eye: "open", wingF: -58, wx: 66, wy: 68 },
      nod:   { crest: "mid", eye: "happy", wingF: 28, spark: [[24, 30, 4], [16, 40, 2.6]] },
      cheer: { crest: "up", eye: "happy", beak: "open", wingF: -32, wx: 66, wy: 68, wingB: -78, spark: [[20, 24, 4.6], [100, 30, 3.6], [24, 78, 2.8]] },
      think: { crest: "mid", eye: "open", ex: .6, ey: -1.2, wingF: -163, wx: 62, wy: 70, dots: [[30, 22, 1.6], [24, 15, 2.2], [17, 7, 2.8]] },
      tea:   { crest: "mid", eye: "happy", wingF: 172, wx: 62, wy: 72, cup: [26, 74] },
      sleep: { crest: "down", eye: "closed", wingF: 28, zz: [90, 34] }
    },
    magician: {
      wave:  { staff: [37, 28, 36, 104], back: [[SL, [39, 82], "fist"], [SR, [89, 45], "open"]] },
      nod:   { eye: "wink", staff: [84, 26, 85, 104], glow: true, back: [[SR, [83.5, 80], "fist"]], front: [[SL, [39, 71], "thumb"]] },
      cheer: { eye: "happy", mouth: "open", brow: "up", staff: [28, 16, 44, 76], glow: true, back: [[SL, [35, 42], "fist"], [SR, [86, 40], "open"]] },
      think: { ex: 1, ey: -1.2, brow: "up", staff: [37, 30, 36, 104], back: [[SL, [38, 82], "fist"]], front: [[SR, [64, 75], "fist"]], dots: [[84, 30, 1.6], [90, 23, 2.2], [97, 15, 2.8]] },
      tea:   { eye: "happy", lean: true, front: [[SL, [54, 82], "none"], [SR, [66, 82], "none"]], cup: [60.2, 81] },
      sleep: { eye: "closed", hat: "droop", lean: true, front: [[SL, [57, 87], "none"], [SR, [63.4, 87.6], "none"]], fold: true, zz: [86, 22] }
    }
  };

  window.Candidates = {
    poses: ["wave", "nod", "cheer", "think", "tea", "sleep"],
    svg: function (who, pose, acc, label) {
      var o = POSES[who][pose], ps = who === "hoopoe" ? hoopoe(o, acc) : magician(o, acc);
      return '<svg viewBox="-4 -10 128 126" role="img" aria-label="' + (label || who + " " + pose) + '"><g transform="rotate(-3 60 60)">' + render(ps) + "</g></svg>";
    }
  };
})();
