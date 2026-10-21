/* Character candidates, round 3: the inked comic style the user picked (blue fog, heavy rough ink,
   deadpan faces). Three characters: the hoopoe «هدهد», the wizard «جادوگر» and the goth lady «بانوی سیاه‌پوش».
   The references use red as the one spot color; here that spot is the user's accent (the app never uses red).
   A turbulence filter roughens every edge so the vectors read as brush ink.
   Usage: Ink.svg("hoopoe" | "wizard" | "goth", "calm" | "big" | "think" | "rest", "#34C47A", { crop: "head" }) → "<svg …>". */
(function () {
  "use strict";
  var INK = "#081018", uid = 0;
  function n(v) { return Math.round(v * 100) / 100; }
  function pt(p) { return n(p[0]) + " " + n(p[1]); }
  function P(d, fill, sw, extra) { return '<path d="' + d + '" fill="' + fill + '"' + (sw ? ' stroke="' + INK + '" stroke-width="' + sw + '" stroke-linejoin="round" stroke-linecap="round"' : "") + (extra || "") + "/>"; }
  function S(d, w, color, extra) { return '<path d="' + d + '" fill="none" stroke="' + (color || INK) + '" stroke-width="' + w + '" stroke-linecap="round" stroke-linejoin="round"' + (extra || "") + "/>"; }
  // a brush stroke: thick in the middle, pointed at both ends, along a quadratic curve
  function brush(a, c, b, w, fill, op) {
    var L = [], R = [], i;
    for (i = 0; i <= 10; i++) {
      var t = i / 10, u = 1 - t;
      var x = u * u * a[0] + 2 * u * t * c[0] + t * t * b[0], y = u * u * a[1] + 2 * u * t * c[1] + t * t * b[1];
      var dx = 2 * u * (c[0] - a[0]) + 2 * t * (b[0] - c[0]), dy = 2 * u * (c[1] - a[1]) + 2 * t * (b[1] - c[1]), l = Math.hypot(dx, dy) || 1;
      var h = w / 2 * Math.sin(Math.PI * (.08 + .84 * t));
      L.push([x - dy / l * h, y + dx / l * h]); R.push([x + dy / l * h, y - dx / l * h]);
    }
    return '<path d="M' + L.map(pt).join("L") + "L" + R.reverse().map(pt).join("L") + 'Z" fill="' + (fill || INK) + '"' + (op != null ? ' opacity="' + op + '"' : "") + "/>";
  }
  function limb(S0, E, w0, w1, fill, sw) {
    var d = [E[0] - S0[0], E[1] - S0[1]], l = Math.hypot(d[0], d[1]), p = [-d[1] / l, d[0] / l];
    var a = [S0[0] + p[0] * w0, S0[1] + p[1] * w0], b = [E[0] + p[0] * w1, E[1] + p[1] * w1], c = [E[0] - p[0] * w1, E[1] - p[1] * w1], e = [S0[0] - p[0] * w0, S0[1] - p[1] * w0];
    var tip = [E[0] + d[0] / l * w1, E[1] + d[1] / l * w1];
    return P("M" + pt(a) + "L" + pt(b) + "Q" + pt(tip) + " " + pt(c) + "L" + pt(e) + "Z", fill, sw == null ? 2.2 : sw);
  }
  function glow(id, x, y, r, acc, op) { return '<circle cx="' + n(x) + '" cy="' + n(y) + '" r="' + n(r) + '" fill="url(#' + id + 'gl)" opacity="' + (op == null ? 1 : op) + '"/>'; }
  function spark(x, y, r, fill) { return P("M" + x + " " + (y - r) + "L" + (x + r * .22) + " " + (y - r * .22) + "L" + (x + r) + " " + y + "L" + (x + r * .22) + " " + (y + r * .22) + "L" + x + " " + (y + r) + "L" + (x - r * .22) + " " + (y + r * .22) + "L" + (x - r) + " " + y + "L" + (x - r * .22) + " " + (y - r * .22) + "Z", fill, 1.2); }
  function zz(x, y) { return S("M" + x + " " + y + "h7l-7 7h7", 2.2, "#E6F5FC") + S("M" + (x + 10) + " " + (y - 10) + "h5l-5 5h5", 1.8, "#E6F5FC"); }
  function shade(hex, t) {
    var v = parseInt(hex.slice(1), 16), r = v >> 16, g = (v >> 8) & 255, b = v & 255;
    function m(c) { return Math.max(0, Math.min(255, Math.round(t > 0 ? c * (1 - t) : c + (255 - c) * -t))); }
    return "#" + ((1 << 24) | (m(r) << 16) | (m(g) << 8) | m(b)).toString(16).slice(1);
  }

  /* ---------- the panel: blue glow, fog, the ink filter and the fade into fog ---------- */
  function defs(id, acc, fadeFrom, fadeTo) {
    return '<radialGradient id="' + id + 'bg" gradientUnits="userSpaceOnUse" cx="100" cy="92" r="190"><stop offset="0" stop-color="#7AD6F5"/><stop offset=".42" stop-color="#2B8FCB"/><stop offset="1" stop-color="#0C355B"/></radialGradient>' +
      '<linearGradient id="' + id + 'fog" gradientUnits="userSpaceOnUse" x1="0" y1="150" x2="0" y2="262"><stop offset="0" stop-color="#A9DEF3" stop-opacity="0"/><stop offset="1" stop-color="#A9DEF3" stop-opacity=".85"/></linearGradient>' +
      '<linearGradient id="' + id + 'fd" gradientUnits="userSpaceOnUse" x1="0" y1="' + fadeFrom + '" x2="0" y2="' + fadeTo + '"><stop offset="0" stop-color="#fff"/><stop offset="1" stop-color="#fff" stop-opacity="0"/></linearGradient>' +
      '<mask id="' + id + 'm" maskUnits="userSpaceOnUse" x="-60" y="-60" width="320" height="380"><rect x="-60" y="-60" width="320" height="380" fill="url(#' + id + 'fd)"/></mask>' +
      '<filter id="' + id + 'ink" x="-10%" y="-10%" width="120%" height="120%"><feTurbulence type="fractalNoise" baseFrequency=".045" numOctaves="2" seed="4"/><feDisplacementMap in="SourceGraphic" scale="2.3" xChannelSelector="R" yChannelSelector="G"/></filter>' +
      '<filter id="' + id + 'blur" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="9"/></filter>' +
      '<radialGradient id="' + id + 'gl"><stop offset="0" stop-color="' + shade(acc, -.5) + '" stop-opacity=".95"/><stop offset=".35" stop-color="' + acc + '" stop-opacity=".45"/><stop offset="1" stop-color="' + acc + '" stop-opacity="0"/></radialGradient>';
  }
  function frame(id, body, front) {
    return '<rect x="-60" y="-60" width="320" height="380" fill="url(#' + id + 'bg)"/>' +
      '<ellipse cx="60" cy="210" rx="70" ry="22" fill="#CBEBF8" opacity=".25" filter="url(#' + id + 'blur)"/>' +
      '<g mask="url(#' + id + 'm)"><g filter="url(#' + id + 'ink)">' + body + "</g></g>" + (front || "") +
      '<rect x="-60" y="140" width="320" height="180" fill="url(#' + id + 'fog)"/>' +
      '<ellipse cx="150" cy="246" rx="80" ry="20" fill="#D6F0FA" opacity=".4" filter="url(#' + id + 'blur)"/>' +
      '<ellipse cx="40" cy="256" rx="70" ry="18" fill="#D6F0FA" opacity=".35" filter="url(#' + id + 'blur)"/>';
  }
  // a deadpan eye: heavy upper lid over the top half; "open" shows the whole eye, "closed" is one line
  function eye(x, y, kind, s, look) {
    s = s || 1; var w = 4.2 * s, lx = (look && look[0]) || 0, ly = (look && look[1]) || 0;
    if (kind === "closed") return S("M" + n(x - w) + " " + y + "Q" + x + " " + n(y + 2.8 * s) + " " + n(x + w) + " " + y, 1.9 * s);
    if (kind === "open") return P("M" + n(x - w) + " " + y + "Q" + x + " " + n(y - 4 * s) + " " + n(x + w) + " " + y + "Q" + x + " " + n(y + 4 * s) + " " + n(x - w) + " " + y + "Z", "#F2FAFD", 1.1 * s) +
      '<circle cx="' + n(x + lx) + '" cy="' + n(y + ly) + '" r="' + n(1.7 * s) + '" fill="' + INK + '"/>';
    return P("M" + n(x - w) + " " + y + "Q" + x + " " + n(y + 3.8 * s) + " " + n(x + w) + " " + y + "Z", "#F2FAFD", 1 * s) +
      '<circle cx="' + n(x + .4 + lx) + '" cy="' + n(y + 1.4 * s + ly * .3) + '" r="' + n(1.45 * s) + '" fill="' + INK + '"/>' +
      S("M" + n(x - w - .6) + " " + n(y - .2) + "Q" + x + " " + n(y - 1.8 * s) + " " + n(x + w + .6) + " " + n(y - .2), 2.1 * s);
  }

  /* ================= the hoopoe ================= */
  var HC = { body: "#D69663", light: "#EFC39A", dark: "#9C5F3A", bw: "#DDEFF8", wood: "#1A2A38" };
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
    var out = [], k = 8;
    for (var i = 0; i < k; i++) {
      var t = i / (k - 1), b = [66 + 26 * t, 70 - 4 * Math.sin(Math.PI * t)], ang, len, w;
      if (kind === "fan") { ang = -158 + 140 * t; len = 31 + 5 * Math.sin(Math.PI * t); w = 4.2; }
      else if (kind === "half") { ang = -118 + 92 * t; len = 24 + 4 * Math.sin(Math.PI * t); w = 3.9; }
      else { var tip = [124, 57 + 1.2 * i]; ang = Math.atan2(tip[1] - b[1], tip[0] - b[0]) * 180 / Math.PI; len = Math.hypot(tip[0] - b[0], tip[1] - b[1]); w = 3.4; }
      out.push([b, ang, len, w]);
    }
    if (kind === "fold") out.reverse();
    return out;
  }
  var H_POSES = {
    calm:  { crest: "fold", tilt: 0, eye: "lid" },
    big:   { crest: "fan", tilt: -7, eye: "open" },
    think: { crest: "half", tilt: -13, eye: "open", look: [.6, -1.1] },
    rest:  { crest: "fold", tilt: 9, eye: "closed" }
  };
  function hoopoe(pose, acc) {
    var o = H_POSES[pose], id = "ih" + (++uid), s = "";
    var df = defs(id, acc, 300, 320) + '<clipPath id="' + id + 'cb"><path d="' + H_BODY + '"/></clipPath><clipPath id="' + id + 'cw"><path d="' + H_WING + '"/></clipPath><clipPath id="' + id + 'ct"><path d="' + H_TAIL + '"/></clipPath>';
    s += '<g transform="translate(0 52)">';
    // gnarled branch with an accent sprout
    s += P("M-10 162C40 154 90 160 130 156C160 153 190 150 214 146L214 156C190 160 160 164 130 166C90 170 40 166 -10 172Z", HC.wood, 2.4);
    s += brush([10, 166], [60, 160], [120, 161], 2.2, "#3E6A8C", .9) + brush([140, 158], [170, 154], [200, 151], 1.6, "#3E6A8C", .9);
    s += S("M58 160C52 150 46 145 36 140", 3, HC.wood) + S("M58 160C52 150 46 145 36 140", 1, "#3E6A8C");
    s += P("M36 140C31 132 23 130 16 133C21 140 29 143 36 140Z", acc, 1.6) + P("M45 146C47 138 44 131 38 128C36 135 39 142 45 146Z", shade(acc, .25), 1.6);
    // tail and legs
    s += P(H_TAIL, INK, 2.2);
    s += '<g clip-path="url(#' + id + 'ct)">' + P("M166 120L178 126L168 152L156 146Z", HC.bw) + "</g>";
    s += S("M104 140L101.5 158M112 141L114 158", 2.8, "#2A3640") + S("M96 159.5Q101 156.5 106 159.5M109 159.5Q114 156.5 119 159.5", 2.4, "#2A3640");
    // body: flat light, one hard cel shadow, a few ink hatches
    s += P(H_BODY, HC.body, 2.4);
    s += '<g clip-path="url(#' + id + 'cb)">' + P("M66 96C70 116 84 128 100 130C84 122 76 110 74 96Z", HC.light) +
      P("M76 128C96 146 126 140 150 116L156 150L60 150Z", HC.dark) +
      brush([90, 132], [104, 138], [118, 134], 1.6) + brush([82, 122], [90, 130], [100, 134], 1.2) + brush([120, 126], [130, 124], [138, 118], 1.4) + "</g>";
    // wing bars
    s += P(H_WING, INK, 2.4);
    s += '<g clip-path="url(#' + id + 'cw)">' +
      P("M90 84L108 88L100 126L82 120Z", HC.body) + P("M90 84L108 88L100 126L82 120Z", HC.dark, 0, ' opacity=".35"') +
      [[116, 6], [129, 6], [142, 5], [153, 4]].map(function (b) { return P("M" + b[0] + " 86L" + (b[0] + b[1]) + " 88L" + (b[0] + b[1] - 14) + " 136L" + (b[0] - 14) + " 134Z", HC.bw); }).join("") + "</g>";
    s += S(H_WING.replace(/Z$/, ""), 2.4);
    // head group
    var g = '<g transform="rotate(' + o.tilt + ' 90 90)">';
    crest(o.crest).forEach(function (f) {
      g += P(feather(f[0], f[1], f[2], f[3]), INK, 1.6);
      g += P(feather(f[0], f[1], f[2] * .74, f[3] * .85), HC.bw);
      g += P(feather(f[0], f[1], f[2] * .62, f[3] * .85), HC.body);
    });
    g += P(H_HEAD, HC.body, 2.4);
    g += P("M66 84C70 90 78 93 86 92C80 90 72 88 66 84Z", HC.dark);
    g += P(H_BILL, "#1E2A33", 2.2) + brush([58, 79], [44, 87], [26, 101], 1.1, "#5D7B90");
    g += eye(73, 74.6, o.eye, .78, o.look);
    g += "</g></g>";
    return { id: id, defs: df, body: s + g, box: { full: "6 22 190 247", head: "18 76 110 110" } };
  }

  /* ================= the wizard ================= */
  var WC = { hat: "#2C80BC", hatD: "#134674", robe: "#2A7DB8", robeD: "#134A78", inner: "#CDE8F5", skin: "#E6AE84", skinD: "#A86E4C", white: "#EEF6FA", whiteD: "#9EC3D8", wood: "#17242E", book: "#1D5F92" };
  var W_POSES = {
    calm:  { eye: "lid", hand: "book", staff: "down", look: [0, 0] },
    big:   { eye: "open", hand: "book", staff: "up", mouth: "open" },
    think: { eye: "lid", hand: "beard", staff: "down", brow: "up", look: [-1.4, 0] },
    rest:  { eye: "closed", hand: "hug", staff: "down", tilt: 5, z: true }
  };
  function staffPath(T, B) {
    var d = [B[0] - T[0], B[1] - T[1]], l = Math.hypot(d[0], d[1]), p = [-d[1] / l, d[0] / l], Lp = [], Rp = [];
    for (var i = 0; i <= 12; i++) {
      var t = i / 12, wob = Math.sin(t * 13) * 1.1, w = 2.8 + Math.sin(t * 7) * .5;
      var c = [T[0] + d[0] * t + p[0] * wob, T[1] + d[1] * t + p[1] * wob];
      Lp.push([c[0] + p[0] * w, c[1] + p[1] * w]); Rp.push([c[0] - p[0] * w, c[1] - p[1] * w]);
    }
    return "M" + Lp.map(pt).join("L") + "L" + Rp.reverse().map(pt).join("L") + "Z";
  }
  function wizard(pose, acc) {
    var o = W_POSES[pose], id = "iw" + (++uid), s = "", f = "";
    var df = defs(id, acc, 180, 252);
    var up = o.staff === "up", T = up ? [52, 26] : [40, 30], B = up ? [70, 252] : [60, 262], H = up ? [58, 104] : [50, 152];
    // staff
    var st = P(staffPath(T, B), WC.wood, 2) + brush([T[0] - 1, T[1] + 20], [T[0] + 2, T[1] + 80], [T[0] + 8, T[1] + 160], 1.2, "#4E7FA3");
    st += S("M" + T[0] + " " + T[1] + "C" + (T[0] - 6) + " " + (T[1] - 8) + " " + (T[0] - 8) + " " + (T[1] - 14) + " " + (T[0] - 13) + " " + (T[1] - 18), 3.4, WC.wood);
    st += S("M" + (T[0] + 1) + " " + T[1] + "C" + (T[0] + 4) + " " + (T[1] - 10) + " " + (T[0] + 10) + " " + (T[1] - 14) + " " + (T[0] + 15) + " " + (T[1] - 16), 3.2, WC.wood);
    st += S("M" + T[0] + " " + (T[1] + 6) + "C" + (T[0] - 8) + " " + (T[1] + 2) + " " + (T[0] - 14) + " " + (T[1] + 2) + " " + (T[0] - 18) + " " + (T[1] - 2), 2.8, WC.wood);
    s += '<g transform="rotate(' + (o.tilt || 0) + ' 104 130)">';
    // hair, robe, inner robe, folds
    s += P("M84 88C78 104 76 124 78 148C84 140 88 128 90 112Z", WC.white, 2) + P("M124 88C132 104 134 124 132 150C126 140 122 128 120 110Z", WC.white, 2);
    s += P("M70 128C60 168 54 214 48 262L160 262C154 214 148 168 138 128C124 118 84 118 70 128Z", WC.robe, 2.4);
    s += P("M92 140C90 180 92 220 94 262L116 262C116 220 118 180 116 140Z", WC.inner, 1.6);
    s += P("M122 126C130 162 136 212 140 262L160 262C154 214 148 168 138 128Z", WC.robeD);
    s += brush([76, 150], [70, 200], [66, 250], 2.2) + brush([132, 160], [138, 210], [144, 258], 2.4) + brush([86, 170], [84, 214], [86, 256], 1.4, WC.robeD);
    // face, nose, brim shadow
    s += P("M88 88C88 100 92 110 100 114C108 116 116 110 120 100C122 94 122 90 122 88Z", WC.skin, 2.2);
    s += P("M88 88L122 88C122 92 121.4 95 120.6 97C110 94 98 94 88.8 97C88.2 94 88 91 88 88Z", WC.skinD);
    var by = o.brow === "up" ? -1.6 : 0;
    s += eye(96, 101, o.eye, .85, o.look) + eye(112, 100.4, o.eye, .85, o.look);
    s += P("M89 " + (96 + by) + "C93 " + (91.6 + by) + " 99 " + (91.4 + by) + " 103.4 " + (94.6 + by) + "C99 " + (94.4 + by) + " 94 " + (95.6 + by) + " 89 " + (98 + by) + "Z", WC.white, 1.3);
    s += P("M107 " + (94 + by) + "C111 " + (90.6 + by) + " 117 " + (90.6 + by) + " 121.6 " + (93.6 + by) + "C117 " + (93.6 + by) + " 112 " + (94.6 + by) + " 107 " + (96.6 + by) + "Z", WC.white, 1.3);
    s += P("M102 95C98 102 93 107 90 111C92 115 98 115 102 111C103.4 107 104.4 101 104.4 96Z", WC.skin, 2) + brush([91.6, 111.6], [96, 112.8], [101, 110.6], 1.1, WC.skinD);
    // beard and mustache
    s += P("M88 106C82 130 84 170 92 210C96 230 100 246 104 262C108 246 112 228 116 208C124 168 126 130 120 106C114 116 106 120 104 120C100 120 94 116 88 106Z", WC.white, 2.4);
    s += brush([112, 124], [118, 170], [110, 222], 3.4, WC.whiteD) + brush([96, 130], [94, 170], [100, 214], 1.4, WC.whiteD) + brush([104, 132], [106, 180], [104, 238], 1.2, WC.whiteD) + brush([90, 120], [88, 150], [92, 180], 1.2, WC.whiteD);
    if (o.mouth === "open") s += P("M101.2 118Q104.2 117 107.2 118Q107.6 121.8 104.2 122.4Q100.8 121.8 101.2 118Z", "#26131A", 1.2);
    else s += S("M100 118.4L108.4 118", 1.6);
    s += P("M88 111C94 107 100 109 104.4 113C108.6 109 116 107 122 111C118 117 110 117 104.4 115C98.6 117 92 117 88 111Z", WC.white, 1.8);
    // hat: cone, bands, brim
    s += P("M76 72C82 54 90 36 102 24C112 14 126 9 140 14C152 19 158 29 166 30C158 36 148 31 138 28C130 27 124 33 122 43C124 53 129 62 134 70C116 74 94 75 76 72Z", WC.hat, 2.4);
    s += P("M80 62C96 66 116 66 130 61L127.4 54.6C114 58.6 96 58.8 83.2 55Z", WC.hatD, 1.4) + P("M96 36C104 39 114 39 121 35L119 30.4C112 33.4 104 33.6 98.6 31Z", WC.hatD, 1.2);
    s += brush([122, 44], [126, 58], [132, 70], 3.2, WC.hatD) + brush([104, 24], [118, 14], [138, 15], 1.4, "#6FB6E2");
    s += P("M30 84C44 70 84 63 114 64C146 65 170 74 176 84C162 92 132 90 104 90C74 90 46 96 30 84Z", WC.hat, 2.4);
    s += brush([40, 86], [100, 92], [168, 86], 3, WC.hatD) + brush([46, 76], [90, 66], [140, 68], 1.2, "#6FB6E2");
    s += "</g>";
    // arms: staff hand, then the other hand's prop
    var SL = [76, 132];
    f += st;
    f += limb(SL, H, 9, 11, WC.robe) + P("M" + (H[0] - 6) + " " + (H[1] - 5) + "C" + (H[0] - 8) + " " + (H[1] + 2) + " " + (H[0] - 5) + " " + (H[1] + 8) + " " + H[0] + " " + (H[1] + 8) + "C" + (H[0] + 6) + " " + (H[1] + 7) + " " + (H[0] + 7) + " " + H[1] + " " + (H[0] + 5) + " " + (H[1] - 5) + "Z", WC.skin, 2) +
      S("M" + (H[0] - 5.6) + " " + (H[1] - 1) + "h9M" + (H[0] - 5.4) + " " + (H[1] + 3) + "h9", 1.2);
    if (up) f += glow(id, T[0] + 1, T[1] - 4, 34, acc) + spark(T[0] + 1, T[1] - 4, 9, shade(acc, -.55)) + spark(T[0] + 22, T[1] + 10, 4, shade(acc, -.4)) + spark(T[0] - 18, T[1] + 18, 3.2, shade(acc, -.4));
    var SR = [132, 134];
    if (o.hand === "book") {
      f += limb(SR, [126, 170], 9, 10, WC.robe);
      f += P("M102 146L136 139L141 173L107 180Z", WC.book, 2.2) + P("M106 150L131 145L135 170L110 175Z", "none", 1.2, ' stroke-opacity=".6"');
      f += '<text x="121" y="162" transform="rotate(-11 121 162)" text-anchor="middle" font-family="Estedad, Vazirmatn, sans-serif" font-weight="800" font-size="8.4" fill="' + shade(acc, -.45) + '">طلسم‌ها</text>';
      f += P("M118 176C116 170 120 166 126 166C132 166 136 170 134 176C132 180 120 180 118 176Z", WC.skin, 2);
    } else if (o.hand === "beard") {
      f += limb(SR, [114, 146], 9, 10, WC.robe) + P("M106 140C104 146 108 152 114 152C120 152 122 146 120 141C116 138 110 137 106 140Z", WC.skin, 2) + S("M108 144h9M108.4 148h8", 1.1);
    } else {
      f += limb(SR, [120, 160], 9, 10, WC.robe) + P("M90 146L124 140L126 162L92 168Z", WC.book, 2.2) + P("M108 156C106 150 110 147 116 147C122 147 125 151 123 156C121 160 110 160 108 156Z", WC.skin, 2);
    }
    if (o.z) f += zz(150, 40);
    return { id: id, defs: df, body: s + f, box: { full: "24 2 160 208", head: "48 14 116 116" } };
  }

  /* ================= the goth lady ================= */
  var GC = { hair: "#0A0F16", hairH: "#2E5E80", skin: "#BCDDEF", skinD: "#7FAACB", dress: "#0E2236", dressH: "#2A5C86", lip: "#24323F" };
  var G_POSES = {
    calm:  { eye: "lid", arms: "down", mouth: "flat", gem: .5 },
    big:   { eye: "lid", arms: "thumb", mouth: "smirk", gem: 1 },
    think: { eye: "lid", arms: "chin", mouth: "flat", gem: .45, look: [2, 0] },
    rest:  { eye: "closed", arms: "mug", mouth: "flat", gem: .3 }
  };
  function hand(x, y, a) {
    return '<g transform="rotate(' + (a || 0) + " " + x + " " + y + ')">' + P("M" + (x - 4) + " " + (y - 3) + "C" + (x - 5) + " " + (y + 4) + " " + (x - 3) + " " + (y + 9) + " " + x + " " + (y + 10) + "C" + (x + 3) + " " + (y + 9) + " " + (x + 5) + " " + (y + 4) + " " + (x + 4) + " " + (y - 3) + "Z", GC.skin, 1.8) +
      S("M" + (x - 1.4) + " " + (y + 3) + "v6M" + (x + 1.4) + " " + (y + 3) + "v6", .9) + "</g>";
  }
  function goth(pose, acc) {
    var o = G_POSES[pose], id = "ig" + (++uid), s = "", f = "";
    var df = defs(id, acc, 176, 250);
    s += P("M62 58C58 36 76 20 100 20C124 20 142 36 138 58C142 110 148 170 152 236L48 236C52 170 58 110 62 58Z", GC.hair, 2.4);
    s += brush([70, 70], [62, 140], [60, 220], 2, GC.hairH, .8) + brush([132, 80], [140, 150], [142, 222], 2, GC.hairH, .8);
    s += P("M92 94L108 94L109 122L91 122Z", GC.skin, 2) + P("M92 96Q100 106 108 96L108.4 104Q100 112 91.6 104Z", GC.skinD);
    // dress: high lace collar, long sleeves, laced front
    s += P("M64 128C74 120 88 118 100 120C112 118 126 120 136 128C138 170 140 214 144 262L56 262C60 214 62 170 64 128Z", GC.dress, 2.4);
    s += brush([74, 140], [70, 200], [68, 258], 2, GC.dressH, .8) + brush([126, 150], [132, 206], [134, 258], 1.6, GC.dressH, .7);
    s += S("M96 140L104 146L96 152L104 158L96 164L104 170L96 176", 1, "#5D8FB5");
    s += P("M86 108L89 112L92 107.6L95 112L98 107.4L101 112L104 107.4L107 112L110 107.6L113 112L115 108L116 124C106 128 94 128 85 124Z", GC.dress, 1.8);
    s += S("M88 117Q100 121 112 117", 1, "#5D8FB5");
    s += glow(id, 100, 133, 10 + 14 * o.gem, acc, .5 + .5 * o.gem) + P("M100 127L104.4 133L100 139L95.6 133Z", shade(acc, -.25), 1.4);
    s += S("M90 125Q100 136 110 125", .9, "#5D8FB5");
    // face
    s += P("M80 58C80 46 88 40 100 40C112 40 120 46 120 58C120 78 112 94 100 98C88 94 80 78 80 58Z", GC.skin, 2.2);
    s += P("M112 66C116 76 114 86 106 94C114 90 118 80 118 68Z", GC.skinD);
    s += eye(91, 72.4, o.eye, .95, o.look) + eye(109, 72.4, o.eye, .95, o.look);
    if (o.eye !== "closed") s += S("M85.6 71.6L82.6 69.6M114.4 71.6L117.4 69.6", 1.6);
    s += S("M87.4 77.4Q91 78.8 94.6 77.4M105.4 77.4Q109 78.8 112.6 77.4", .8, GC.skinD);
    s += S("M100.6 77L99 83.2L101.6 83.8", 1);
    s += '<circle cx="92.4" cy="83.4" r=".9" fill="' + INK + '"/>';
    if (o.mouth === "smirk") s += P("M95 89.4Q101 88.6 106 86.4Q103 91.6 96.6 91Z", GC.lip, 1) + S("M105.4 86.6l1.6 -1", 1);
    else s += P("M95.4 88.6Q100 87.2 104.6 88.6Q100 91 95.4 88.6Z", GC.lip, 1);
    // side curtains and blunt bangs
    s += P("M80 52C76 80 78 110 72 150L84 148C86 110 86 82 86 60Z", GC.hair, 2) + P("M120 52C124 80 122 110 128 150L116 148C114 110 114 82 114 60Z", GC.hair, 2);
    s += P("M78 64C74 38 88 26 100 26C114 26 126 38 122 64L117 61L113 66L108 61L103 66L98 61L93 66L88 61L83 66Z", GC.hair, 2.2);
    s += brush([86, 34], [98, 28], [112, 32], 1.6, GC.hairH, .9) + brush([84, 44], [90, 36], [98, 34], 1, GC.hairH, .7);
    // arms
    var SL = [68, 134], SR = [132, 134];
    if (o.arms === "mug") {
      f += limb(SL, [88, 150], 9, 6.6, GC.dress) + limb(SR, [112, 150], 9, 6.6, GC.dress) + '<g transform="translate(0 -16)">';
      f += P("M90 154h20l-2 18q-.6 3 -3.6 3h-8.8q-3 0 -3.6 -3Z", "#D7EDF8", 2) + S("M110 158q6 0 5 6q-1 4 -6 3.6", 2) + P("M90.6 156.6h18.8", "none", 1);
      f += hand(88, 164, 60) + hand(112, 164, -60);
      f += brush([96, 150], [92, 142], [97, 134], 1.4, "#E6F5FC") + brush([104, 150], [100, 140], [106, 132], 1.2, "#E6F5FC") + "</g>";
    } else {
      f += limb(SL, [60, 204], 9, 6.6, GC.dress) + hand(60, 206, 4) + S("M57 196l7 1", 1.4, "#5D8FB5");
      if (o.arms === "thumb") {
        f += limb(SR, [140, 172], 9, 7, GC.dress) + limb([140, 172], [130, 156], 7, 6, GC.dress);
        f += P("M125.6 147C124.6 140 125.6 135.6 128.6 134.4C131.4 134 132.2 137 131 140.6L130.6 147Z", GC.skin, 1.6);
        f += P("M123 146.4C121.6 152 124 157.4 130 157.6C136 157.6 138.6 153 137.6 147.6C135 145 127 144.6 123 146.4Z", GC.skin, 1.8) + S("M126 150.6h9.4M126 154h8.6", .9);
      } else if (o.arms === "chin") {
        f += limb(SR, [140, 168], 9, 7, GC.dress) + limb([140, 168], [112, 102], 7, 6, GC.dress) + hand(108, 94, 150);
      } else f += limb(SR, [140, 204], 9, 6.6, GC.dress) + hand(140, 206, -4) + S("M136 196l7 1", 1.4, "#5D8FB5");
    }
    return { id: id, defs: df, body: s + f, box: { full: "38 14 124 161", head: "52 18 96 96" } };
  }

  window.Ink = {
    poses: ["calm", "big", "think", "rest"],
    svg: function (who, pose, acc, opt) {
      opt = opt || {};
      var r = who === "hoopoe" ? hoopoe(pose, acc) : who === "wizard" ? wizard(pose, acc) : goth(pose, acc);
      return '<svg viewBox="' + r.box[opt.crop || "full"] + '" role="img" aria-label="' + (opt.label || who) + '"><defs>' + r.defs + "</defs>" + frame(r.id, r.body) + "</svg>";
    }
  };
})();
