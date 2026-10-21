/* Character candidates, round 2: grown-up. The hoopoe «هدهد» and the old magician «پیر» drawn with
   natural proportions, real colors and soft light from the upper left: no sticker border, no grins.
   Texture (feather marks, beard strands, robe folds) comes from a seeded random, so every render is the same.
   Usage: Real.svg("hoopoe" | "magician", "calm" | "big" | "think" | "rest", "#34C47A", { crop: "head" }) → "<svg …>". */
(function () {
  "use strict";
  var uid = 0;
  function n(v) { return Math.round(v * 100) / 100; }
  function pt(p) { return n(p[0]) + " " + n(p[1]); }
  function rng(seed) {
    return function () {
      seed = (seed + 0x6D2B79F5) | 0;
      var t = Math.imul(seed ^ (seed >>> 15), 1 | seed);
      t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
      return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
    };
  }
  function stops(s) { return s.map(function (x) { return '<stop offset="' + x[0] + '" stop-color="' + x[1] + '"' + (x[2] != null ? ' stop-opacity="' + x[2] + '"' : "") + "/>"; }).join(""); }
  function lin(id, x1, y1, x2, y2, s) { return '<linearGradient id="' + id + '" gradientUnits="userSpaceOnUse" x1="' + x1 + '" y1="' + y1 + '" x2="' + x2 + '" y2="' + y2 + '">' + stops(s) + "</linearGradient>"; }
  function rad(id, cx, cy, r, s, fx, fy) { return '<radialGradient id="' + id + '" gradientUnits="userSpaceOnUse" cx="' + cx + '" cy="' + cy + '" r="' + r + '"' + (fx != null ? ' fx="' + fx + '" fy="' + fy + '"' : "") + ">" + stops(s) + "</radialGradient>"; }
  function path(d, fill, extra) { return '<path d="' + d + '" fill="' + fill + '"' + (extra || "") + "/>"; }
  function line(d, color, w, extra) { return '<path d="' + d + '" fill="none" stroke="' + color + '" stroke-width="' + w + '" stroke-linecap="round" stroke-linejoin="round"' + (extra || "") + "/>"; }
  function shade(hex, t) {
    var v = parseInt(hex.slice(1), 16), r = v >> 16, g = (v >> 8) & 255, b = v & 255;
    function m(c) { return Math.max(0, Math.min(255, Math.round(t > 0 ? c * (1 - t) : c + (255 - c) * -t))); }
    return "#" + ((1 << 24) | (m(r) << 16) | (m(g) << 8) | m(b)).toString(16).slice(1);
  }

  /* ================= the hoopoe ================= */
  var HC = { light: "#EBBA92", mid: "#D99A6C", dark: "#B97549", breast: "#F4D5B2", black: "#1D1918", white: "#F3ECE1", bill: "#231E1C", leg: "#4E4845", wood: "#5C4535" };
  var H_HEAD = "M60 74C62 64 73 58 84 60C94 62 99 71 98 80C97 89 89 94 79 94C70 94 63 89 62 84Z";
  var H_BODY = "M86 63C98 70 104 80 112 88C124 98 136 106 146 114L148 122C140 134 124 144 106 144C88 144 74 132 68 116C64 104 62 94 64 86C72 90 80 86 86 63Z";
  var H_WING = "M95 89C112 92 137 104 161 124C151 131 132 133 116 129C104 125 96 113 93 101Z";
  var H_TAIL = "M139 112C158 118 178 128 196 140C194 146 190 148 185 147C168 139 151 132 137 126Z";
  var H_BILL = "M61 74.5C50 79 34 91 16 108C15.4 109 16.4 109.8 17.6 109C34 97 49 87 62.5 83.5Z";

  function feather(b, ang, len, w) {
    var a = ang * Math.PI / 180, d = [Math.cos(a), Math.sin(a)], p = [-d[1], d[0]];
    function q(t, s) { return pt([b[0] + d[0] * len * t + p[0] * w * s, b[1] + d[1] * len * t + p[1] * w * s]); }
    return "M" + q(0, -.7) + "C" + q(.35, -1.1) + " " + q(.8, -1.15) + " " + q(.97, -.45) + "Q" + q(1.05, 0) + " " + q(.97, .45) + "C" + q(.8, 1.15) + " " + q(.35, 1.1) + " " + q(0, .7) + "Z";
  }
  function crest(kind) {
    var out = [], i, k = 9;
    for (i = 0; i < k; i++) {
      var t = i / (k - 1), b = [66 + 26 * t, 70 - 4 * Math.sin(Math.PI * t)], ang, len, w;
      if (kind === "fan") { ang = -158 + 140 * t; len = 31 + 5 * Math.sin(Math.PI * t); w = 3.9; }
      else if (kind === "half") { ang = -118 + 92 * t; len = 24 + 4 * Math.sin(Math.PI * t); w = 3.6; }
      else { var tip = [124, 57 + 1.2 * i]; ang = Math.atan2(tip[1] - b[1], tip[0] - b[0]) * 180 / Math.PI; len = Math.hypot(tip[0] - b[0], tip[1] - b[1]); w = 3.1; }
      out.push([b, ang, len, w]);
    }
    if (kind === "fold") out.reverse(); // the forehead feathers lie on top
    return out;
  }
  function bands(A, B, list, reach) {
    var d = [B[0] - A[0], B[1] - A[1]], l = Math.hypot(d[0], d[1]), p = [-d[1] / l * reach, d[0] / l * reach];
    return list.map(function (s) {
      var a = [A[0] + d[0] * s[0], A[1] + d[1] * s[0]], b = [A[0] + d[0] * s[1], A[1] + d[1] * s[1]];
      return path("M" + pt([a[0] + p[0], a[1] + p[1]]) + "L" + pt([b[0] + p[0], b[1] + p[1]]) + "L" + pt([b[0] - p[0], b[1] - p[1]]) + "L" + pt([a[0] - p[0], a[1] - p[1]]) + "Z", s[2]);
    }).join("");
  }
  var H_POSES = {
    calm:  { crest: "fold", tilt: 0, eye: "open" },
    big:   { crest: "fan", tilt: -7, eye: "open" },
    think: { crest: "half", tilt: -13, eye: "open" },
    rest:  { crest: "fold", tilt: 9, eye: "closed" }
  };
  function hoopoe(pose, acc) {
    var o = H_POSES[pose], id = "h" + (++uid), R = rng(7), defs = "", s = "";
    defs += lin(id + "b", 70, 66, 140, 140, [[0, HC.light], [.55, HC.mid], [1, HC.dark]]);
    defs += rad(id + "br", 80, 112, 30, [[0, HC.breast, .95], [1, HC.breast, 0]]);
    defs += rad(id + "sh", 128, 126, 34, [[0, "#5A3420", .35], [1, "#5A3420", 0]]);
    defs += lin(id + "bill", 62, 79, 16, 108, [[0, "#5C4C46"], [.35, HC.bill], [1, "#0F0C0B"]]);
    defs += lin(id + "wd", 30, 156, 30, 168, [[0, "#6E5444"], [1, "#3C2C22"]]);
    defs += '<clipPath id="' + id + 'cb"><path d="' + H_BODY + '"/><path d="' + H_HEAD + '"/></clipPath>';
    defs += '<clipPath id="' + id + 'cw"><path d="' + H_WING + '"/></clipPath>';
    defs += '<clipPath id="' + id + 'ct"><path d="' + H_TAIL + '"/></clipPath>';

    // branch, with a sprout in the accent
    s += path("M24 161C70 156 120 158 198 151L198 158C120 165 70 164 24 168Z", "url(#" + id + "wd)");
    s += line("M58 160C52 152 46 147 38 143", "#4A372B", 2.2);
    s += path("M38 143C34 136 27 134 21 136C25 142 31 145 38 143Z", acc);
    s += path("M46 148C48 141 46 135 41 132C39 138 41 144 46 148Z", shade(acc, .25));
    // tail and legs behind the body
    s += path(H_TAIL, HC.black);
    s += '<g clip-path="url(#' + id + 'ct)">' + bands([139, 119], [196, 144], [[.5, .62, HC.white]], 20) + "</g>";
    s += line("M104 140L101.5 158M112 141L114 158", HC.leg, 2.4);
    s += line("M96 159.5Q101 157 106 159.5M109 159.5Q114 157 119 159.5", HC.leg, 2);
    // body
    s += path(H_BODY, "url(#" + id + "b)");
    s += '<g clip-path="url(#' + id + 'cb)"><circle cx="80" cy="112" r="30" fill="url(#' + id + 'br)"/><circle cx="128" cy="126" r="34" fill="url(#' + id + 'sh)"/>';
    for (var i = 0; i < 34; i++) {
      var x = 72 + R() * 46, y = 98 + R() * 42, l = 2 + R() * 2.5;
      s += line("M" + n(x) + " " + n(y) + "l" + n(l) + " " + n(l * .45), y > 126 ? "#7A4A2C" : "#B57E57", .6, ' opacity="' + n(.12 + R() * .18) + '"');
    }
    s += "</g>";
    // wing: buff coverts, then the black-and-white bars
    s += path(H_WING, HC.black);
    s += '<g clip-path="url(#' + id + 'cw)">' + bands([94, 95], [161, 125], [
      [-.2, .2, "#C8875C"], [.2, .27, HC.black], [.27, .34, HC.white], [.34, .45, HC.black], [.45, .52, HC.white],
      [.52, .63, HC.black], [.63, .69, HC.white], [.69, .8, HC.black], [.8, .84, HC.white]], 40);
    s += line("M122 118C134 120 146 122 158 124M118 123C130 126 142 128 154 128", "#3A3330", .7, ' opacity=".8"') + "</g>";
    s += line(H_WING.replace(/Z$/, ""), "#120F0E", .8, ' opacity=".6"');

    // head group: tilts for the pose
    var g = '<g transform="rotate(' + o.tilt + ' 90 90)">';
    crest(o.crest).forEach(function (f) {
      g += path(feather(f[0], f[1], f[2], f[3]), HC.black);
      g += path(feather(f[0], f[1], f[2] * .74, f[3] * .92), HC.white);
      g += path(feather(f[0], f[1], f[2] * .63, f[3] * .92), "url(#" + id + "b)");
    });
    g += path(H_HEAD, "url(#" + id + "b)");
    g += path(H_HEAD, "url(#" + id + "br)", ' opacity=".5"');
    g += path(H_BILL, "url(#" + id + "bill)");
    g += line("M62 77C66 76 69 75 71 74.6", "#2B201C", 1, ' opacity=".55"');
    if (o.eye === "closed") g += line("M69.2 74.6Q72 76.8 74.8 74.6", "#2B201C", 1.1);
    else g += '<circle cx="72" cy="74.4" r="2.5" fill="#1A1311"/><circle cx="72.8" cy="73.6" r=".75" fill="#FFF8EE"/>';
    g += "</g>";
    return { defs: defs, body: s + g, box: { full: "10 14 190 160", head: "26 34 98 98" } };
  }

  /* ================= the old magician ================= */
  var MC = { robeL: "#5C697F", robeD: "#2E3646", hatL: "#606A7C", hatD: "#30374A", skin: "#C49576", skinD: "#4A362D", beardL: "#E6E3DC", beardD: "#B3AEA5", hair: "#A9A6A0", wood: "#5E4130", rope: "#8B7755", boot: "#2A211C" };
  var M_FACE = "M86 57L112 57C114 68 114 78 110 86C104 92 92 92 86 86C84 78 84 68 86 57Z";
  var M_BEARD = "M85 82C80 98 82 118 88 134C92 146 95 156 97 167C101 154 107 142 111 126C115 108 114 94 111 82C104 90 92 90 85 82Z";
  var M_ROBE = "M76 92C68 122 62 162 52 194C82 199 122 199 150 194C140 162 134 122 126 92C116 86 86 86 76 92Z";
  var M_POSES = {
    calm:  { staff: [[148, 120], [152, 24], [146, 196]], left: [74, 146], glow: .55, eye: "open", tilt: 0 },
    big:   { staff: [[144, 66], [150, 16], [132, 178]], left: [74, 146], glow: 1, eye: "open", tilt: -3 },
    think: { staff: [[148, 120], [152, 24], [146, 196]], left: [95, 121], front: true, glow: .4, eye: "open", tilt: 2 },
    rest:  { staff: [[148, 120], [152, 24], [146, 196]], left: [74, 146], glow: .15, eye: "closed", tilt: 5 }
  };
  function sleeve(S, H, wS, wC, droop) {
    var d = [H[0] - S[0], H[1] - S[1]], l = Math.hypot(d[0], d[1]), u = [d[0] / l, d[1] / l], p = [-u[1], u[0]];
    if (p[1] > 0 || (p[1] === 0 && p[0] > 0)) p = [-p[0], -p[1]];
    var M = [S[0] + d[0] * .5, S[1] + d[1] * .5];
    var s1 = [S[0] + p[0] * wS, S[1] + p[1] * wS], s2 = [S[0] - p[0] * wS, S[1] - p[1] * wS];
    var c1 = [H[0] + p[0] * wC, H[1] + p[1] * wC], c2 = [H[0] - p[0] * (wC + 3), H[1] - p[1] * (wC + 3) + droop];
    return { d: "M" + pt(s1) + "Q" + pt([M[0] + p[0] * (wS + 2), M[1] + p[1] * (wS + 2)]) + " " + pt(c1) + "L" + pt(c2) + "Q" + pt([M[0] - p[0] * (wS + 4), M[1] - p[1] * (wS + 4) + droop * .6]) + " " + pt(s2) + "Z", u: u, p: p, c1: c1, c2: c2 };
  }
  function arm(S, H, id, droop) {
    var sl = sleeve(S, H, 8, 8, droop);
    var hand = [H[0] + sl.u[0] * 2.5, H[1] + sl.u[1] * 2.5];
    return path(sl.d, "url(#" + id + "r)", ' stroke="#1A202B" stroke-width=".8" stroke-opacity=".7"') +
      line("M" + pt(sl.c1) + "L" + pt(sl.c2), "#1E2430", 2.2, ' opacity=".7"') +
      '<ellipse cx="' + n(hand[0]) + '" cy="' + n(hand[1]) + '" rx="4.6" ry="5.2" fill="url(#' + id + 'hs)" transform="rotate(' + n(Math.atan2(sl.u[1], sl.u[0]) * 180 / Math.PI - 90) + " " + n(hand[0]) + " " + n(hand[1]) + ')"/>' +
      line("M" + pt([hand[0] - 3, hand[1] - 1]) + "q3 1.6 6 0", "#7E5A46", .7, ' opacity=".7"');
  }
  function staff(T, B, id, glow) {
    var s = "", d = [B[0] - T[0], B[1] - T[1]], l = Math.hypot(d[0], d[1]), u = [d[0] / l, d[1] / l], p = [-u[1], u[0]], pts = [];
    for (var i = 0; i <= 8; i++) { var t = i / 8, w = Math.sin(t * 9) * .9; pts.push([T[0] + d[0] * t + p[0] * w, T[1] + d[1] * t + p[1] * w]); }
    s += line("M" + pts.map(pt).join("L"), MC.wood, 3.8);
    s += line("M" + pts.map(function (q) { return pt([q[0] - p[0] * .9, q[1] - p[1] * .9]); }).join("L"), "#86603F", 1, ' opacity=".7"');
    [.3, .62].forEach(function (t) { var k = [T[0] + d[0] * t, T[1] + d[1] * t]; s += '<ellipse cx="' + n(k[0] + p[0] * 1.6) + '" cy="' + n(k[1] + p[1] * 1.6) + '" rx="1.6" ry="2.4" fill="#3E2B20"/>'; });
    var st = [T[0] - u[0] * 7, T[1] - u[1] * 7];
    s += '<circle cx="' + n(st[0]) + '" cy="' + n(st[1]) + '" r="' + n(12 + 18 * glow) + '" fill="url(#' + id + 'gl)" opacity="' + n(.35 + .65 * glow) + '"/>';
    s += line("M" + pt(T) + "C" + pt([T[0] + p[0] * 6, T[1] + p[1] * 6]) + " " + pt([st[0] + p[0] * 8, st[1] + p[1] * 8]) + " " + pt([st[0] + p[0] * 3 - u[0] * 6, st[1] + p[1] * 3 - u[1] * 6]), MC.wood, 2.2);
    s += line("M" + pt(T) + "C" + pt([T[0] - p[0] * 6, T[1] - p[1] * 6]) + " " + pt([st[0] - p[0] * 8, st[1] - p[1] * 8]) + " " + pt([st[0] - p[0] * 3 - u[0] * 6, st[1] - p[1] * 3 - u[1] * 6]), MC.wood, 2.2);
    s += '<circle cx="' + n(st[0]) + '" cy="' + n(st[1]) + '" r="5" fill="url(#' + id + 'st)"/>';
    if (glow > .9) for (var r = 0; r < 8; r++) {
      var a = r * Math.PI / 4 + .3, r1 = 9, r2 = 16 + (r % 2) * 6;
      s += line("M" + pt([st[0] + Math.cos(a) * r1, st[1] + Math.sin(a) * r1]) + "L" + pt([st[0] + Math.cos(a) * r2, st[1] + Math.sin(a) * r2]), "url(#" + id + "ray)", 1.1, ' opacity=".8"');
    }
    return s;
  }
  function magician(pose, acc) {
    var o = M_POSES[pose], id = "m" + (++uid), R = rng(11), defs = "", s = "", i;
    defs += lin(id + "r", 62, 0, 142, 0, [[0, MC.robeL], [1, MC.robeD]]);
    defs += lin(id + "h", 70, 0, 140, 0, [[0, MC.hatL], [1, MC.hatD]]);
    defs += lin(id + "f", 0, 58, 0, 88, [[0, MC.skinD], [.42, "#9A735C"], [1, MC.skin]]);
    defs += lin(id + "bd", 0, 84, 0, 166, [[0, MC.beardL], [1, MC.beardD]]);
    defs += rad(id + "hs", 0, 0, 1, [[0, "#D7A887"], [1, "#B98868"]]).replace('cx="0" cy="0" r="1"', 'cx="50%" cy="40%" r="60%"').replace('gradientUnits="userSpaceOnUse" ', "");
    defs += '<radialGradient id="' + id + 'st" cx="40%" cy="35%" r="70%"><stop offset="0" stop-color="#FFFFFF"/><stop offset=".35" stop-color="' + shade(acc, -.45) + '"/><stop offset="1" stop-color="' + shade(acc, .35) + '"/></radialGradient>';
    defs += '<radialGradient id="' + id + 'gl"><stop offset="0" stop-color="' + acc + '" stop-opacity=".75"/><stop offset=".4" stop-color="' + acc + '" stop-opacity=".25"/><stop offset="1" stop-color="' + acc + '" stop-opacity="0"/></radialGradient>';
    defs += '<linearGradient id="' + id + 'ray"><stop offset="0" stop-color="' + shade(acc, -.5) + '"/><stop offset="1" stop-color="' + acc + '"/></linearGradient>';
    defs += '<clipPath id="' + id + 'cr"><path d="' + M_ROBE + '"/></clipPath>';
    defs += '<clipPath id="' + id + 'cbd"><path d="' + M_BEARD + '"/></clipPath>';

    s += '<ellipse cx="100" cy="196" rx="54" ry="4.5" fill="#000" opacity=".3"/>';
    // hair falling behind, then the robe
    s += path("M108 57C121 63 127 82 125 109C120 105 114 97 110 88Z", MC.hair);
    s += path(M_ROBE, "url(#" + id + "r)");
    s += '<g clip-path="url(#' + id + 'cr)">';
    [["M90 108C88 140 86 170 82 198", .35], ["M112 106C115 140 119 170 124 198", .4], ["M100 140C101 160 101 180 102 198", .3], ["M74 120C70 150 66 176 62 198", .25]].forEach(function (f) { s += line(f[0], "#1C2230", 1.4, ' opacity="' + f[1] + '"'); });
    s += line("M86 110C86 140 84 168 80 198", "#8090A8", .9, ' opacity=".25"');
    s += "</g>";
    s += '<ellipse cx="81" cy="195.5" rx="9" ry="3.4" fill="' + MC.boot + '"/><ellipse cx="115" cy="196" rx="9" ry="3.2" fill="' + MC.boot + '"/>';
    s += line("M80 132C94 137 110 137 124 131", MC.rope, 2.4) + line("M104 136C105 146 103 156 105 166", MC.rope, 1.8) + '<ellipse cx="105" cy="168" rx="1.8" ry="3" fill="' + MC.rope + '"/>';
    if (!o.front) s += arm([80, 98], o.left, id, 8);

    // head group
    var g = '<g transform="rotate(' + o.tilt + ' 98 94)">';
    g += path("M86 57C79 64 77 75 80 83L86 80Z", MC.hair);
    g += path(M_FACE, "url(#" + id + "f)");
    g += path("M92 65.5C90 72 86 76 83 80C83.6 82.6 86.2 83.6 89 82.6C91 82 92.4 80 93.4 78.6Z", "#B9886A");
    g += line("M91.6 67C90 72 87.6 75.6 85 79", "#D8AC8C", .9, ' opacity=".6"');
    if (o.eye === "closed") g += line("M86 71Q88.4 72.6 90.6 71M100 70.6Q103 72.4 106 70.6", "#2A1D18", .9);
    else g += '<ellipse cx="88.4" cy="70.6" rx="1.7" ry="1.05" fill="#1A1210"/><ellipse cx="103" cy="70.4" rx="2.3" ry="1.25" fill="#1A1210"/><circle cx="89" cy="70.2" r=".5" fill="#F4EBDD"/><circle cx="103.8" cy="69.9" r=".65" fill="#F4EBDD"/>';
    g += path("M84.5 67.6C87 65 91 64.6 93 66.4C90.6 66.8 87.4 67.6 84.5 67.6Z", MC.beardL);
    g += path("M97 66.6C101 63.6 107 63.8 110.6 66.6C106.6 66.6 101 67.6 97 66.6Z", MC.beardL);
    g += path(M_BEARD, "url(#" + id + "bd)");
    g += '<g clip-path="url(#' + id + 'cbd)">';
    for (i = 0; i < 26; i++) {
      var x0 = 84 + R() * 28, cx = 88 + R() * 20, tipx = 92 + R() * 10;
      g += line("M" + n(x0) + " " + n(84 + R() * 6) + "Q" + n(cx) + " " + n(120 + R() * 20) + " " + n(tipx) + " " + n(150 + R() * 18), i % 3 ? "#9C978E" : "#FFFFFF", .55, ' opacity="' + n(.35 + R() * .35) + '"');
    }
    g += path("M104 84C110 100 110 120 104 140C112 124 114 104 112 84Z", "#8E897F", ' opacity=".35"') + "</g>";
    g += path("M84 85C88 81 94 81 97 84C101 81 108 82 112 87C108 90 102 90 98 88C94 91 88 90 84 85Z", "#D9D5CD");
    g += line("M86 85.6C90 84 94 84.4 97 86M98.6 86C102 84.6 106 85 110 87", "#A49F96", .6, ' opacity=".7"');
    // hat: cone, band, brim
    g += path("M78 53C84 40 92 24 104 12C110 6 119 3 126 6C119 9 114 13 112 19C114 31 118 43 124 53C108 56 92 56 78 53Z", "url(#" + id + "h)");
    g += line("M96 30C100 33 106 33 110 30M90 42C97 45 107 45 115 41", "#20263A", .9, ' opacity=".45"');
    g += line("M104 14C110 9 117 6 124 6", "#7C869A", .8, ' opacity=".45"');
    g += path("M79 51C94 55 110 55 123 51L121.5 46C108 50 94 50 80.5 46Z", "#262C3B");
    g += path("M46 62C58 54 82 49 102 49C124 49 144 54 156 61C150 65 132 63 112 62C94 61 68 63 46 62Z", "url(#" + id + "h)");
    g += path("M46 62C68 63 94 61 112 62C132 63 150 65 156 61C150 67 130 67 110 66C90 66 66 67 46 62Z", "#22283A");
    g += "</g>";

    s += g;
    if (o.front) s += arm([80, 98], o.left, id, 2);
    var sf = o.staff;
    s += staff(sf[1], sf[2], id, o.glow);
    s += arm([122, 98], sf[0], id, 4);
    return { defs: defs, body: s, box: { full: "16 -10 172 210", head: "56 16 86 86" } };
  }

  window.Real = {
    poses: ["calm", "big", "think", "rest"],
    svg: function (who, pose, acc, opt) {
      opt = opt || {};
      var r = who === "hoopoe" ? hoopoe(pose, acc) : magician(pose, acc);
      return '<svg viewBox="' + r.box[opt.crop || "full"] + '" role="img" aria-label="' + (opt.label || who) + '"><defs>' + r.defs + "</defs>" + r.body + "</svg>";
    }
  };
})();
