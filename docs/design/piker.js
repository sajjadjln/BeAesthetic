/* «پیکر», the character (D82). One rig draws every pose, face and outfit as an SVG string.
   Same construction as the logo (D81): simple shapes drawn four times — drop shadow, white sticker
   border, ink outline, body color — so every pose reads as the same sticker. The body takes the
   user's accent; the launcher icon stays green.
   Usage: Piker.svg({ pose: "wave", color: "#34C47A", outfit: "band", say: false }) → "<svg …>". */
(function () {
  "use strict";
  var INK = "#0E1110", PAPER = "#F7F8F7", SHADOW = "#000";
  var BORDER = 5.8, OUTLINE = 2.2;

  /* ---------- primitives: c = circle, s = segment, p = path, r = rect, e = ellipse ---------- */
  function n(v) { return Math.round(v * 100) / 100; }
  function prim(sh, fill, b) {
    var st = b ? ' stroke="' + fill + '" stroke-width="' + n(2 * b) + '" stroke-linejoin="round"' : "";
    switch (sh[0]) {
      case "c": return '<circle cx="' + n(sh[1]) + '" cy="' + n(sh[2]) + '" r="' + n(sh[3]) + '" fill="' + fill + '"' + st + "/>";
      case "e": return '<ellipse cx="' + n(sh[1]) + '" cy="' + n(sh[2]) + '" rx="' + n(sh[3]) + '" ry="' + n(sh[4]) + '" fill="' + fill + '"' + st + "/>";
      case "r": return '<rect x="' + n(sh[1]) + '" y="' + n(sh[2]) + '" width="' + n(sh[3]) + '" height="' + n(sh[4]) + '" rx="' + n(sh[5] || 0) + '" fill="' + fill + '"' + st + (sh[6] ? ' transform="' + sh[6] + '"' : "") + "/>";
      case "s": return '<path d="M' + n(sh[1]) + " " + n(sh[2]) + "L" + n(sh[3]) + " " + n(sh[4]) + '" stroke="' + fill + '" stroke-width="' + n(sh[5] + 2 * b) + '" stroke-linecap="round" fill="none"/>';
      default: return '<path d="' + sh[1] + '" fill="' + fill + '"' + st + "/>";
    }
  }
  function all(list, fill, b) { return list.map(function (s) { return prim(s, fill, b); }).join(""); }
  function star(x, y, r, fill) { return '<path d="M' + x + " " + (y - r) + "Q" + x + " " + y + " " + (x + r) + " " + y + "Q" + x + " " + y + " " + x + " " + (y + r) + "Q" + x + " " + y + " " + (x - r) + " " + y + "Q" + x + " " + y + " " + x + " " + (y - r) + 'Z" fill="' + (fill || "#FFFFFF") + '"/>'; }

  /* ---------- arms ---------- */
  var SH = { L: [37, 52], R: [71, 52] };
  function unit(ax, ay) { var l = Math.hypot(ax, ay) || 1; return [ax / l, ay / l]; }
  function rot(v, deg) { var a = deg * Math.PI / 180; return [v[0] * Math.cos(a) - v[1] * Math.sin(a), v[0] * Math.sin(a) + v[1] * Math.cos(a)]; }
  // a = { e:[x,y] elbow, h:[x,y] hand center, hand:"fist|open|thumb|point|none", bi:0..1 biceps }
  function arm(side, a) {
    var S = SH[side], E = a.e, H = a.h, bi = a.bi == null ? .4 : a.bi;
    var d = unit(E[0] - S[0], E[1] - S[1]), f = unit(H[0] - E[0], H[1] - E[1]);
    var p = [-d[1], d[0]];
    if (p[0] * f[0] + p[1] * f[1] < 0) p = [d[1], -d[0]];
    var M = [(S[0] + E[0]) / 2, (S[1] + E[1]) / 2], off = 2.4 + 4.3 * bi, br = 5.8 + 2.4 * bi;
    var B = [M[0] + p[0] * off, M[1] + p[1] * off];
    var Hc = [H[0] - f[0] * 3.5, H[1] - f[1] * 3.5];
    var upper = [["s", S[0], S[1], E[0], E[1], 10], ["c", B[0], B[1], br]], out = [["s", E[0], E[1], Hc[0], Hc[1], 9]];
    var hand = a.hand || "fist";
    if (hand !== "none") out.push(["c", H[0], H[1], hand === "open" ? 6.2 : 6.4]);
    if (hand === "open") [-38, 0, 38].forEach(function (g) { var v = rot(f, g); out.push(["c", H[0] + v[0] * 6, H[1] + v[1] * 6, 2.5]); });
    if (hand === "open") { var t = rot(f, side === "L" ? -95 : 95); out.push(["c", H[0] + t[0] * 5.6, H[1] + t[1] * 5.6, 2.5]); }
    if (hand === "thumb") out.push(["s", H[0] + .5, H[1] - 3, H[0] + 1.2, H[1] - 11, 4.6]);
    if (hand === "point") out.push(["s", H[0], H[1], H[0] + f[0] * 10, H[1] + f[1] * 10, 4.2]);
    // A front arm keeps its upper arm behind (merged with the body); only the forearm and hand cross in front.
    return { upper: upper, lower: out, shine: [B[0] - 2.9, B[1] - 3.3] };
  }

  /* ---------- body ---------- */
  var TRAPS = ["p", "M46 44h16l5 6.5H41z"];
  var TORSO = ["p", "M35 51C35 45.5 73 45.5 73 51C76 57 72 66 66 73C63.5 77 62 81 61 84L47 84C46 81 44.5 77 42 73C36 66 32 57 35 51Z"];
  function body(hx, hy) { return [TRAPS, TORSO, ["c", 38, 51, 7.5], ["c", 70, 51, 7.5], ["c", 54 + hx, 34 + hy, 10.5]]; }

  /* ---------- faces ---------- */
  var EYES = {
    open: '<g fill="' + INK + '"><ellipse cx="50.2" cy="34" rx="1.5" ry="1.9"/><ellipse cx="57.8" cy="34" rx="1.5" ry="1.9"/></g>',
    wide: '<g fill="' + INK + '"><circle cx="50" cy="34" r="2.3"/><circle cx="58" cy="34" r="2.3"/></g><g fill="#fff"><circle cx="50.8" cy="33.1" r=".75"/><circle cx="58.8" cy="33.1" r=".75"/></g>',
    happy: '<path d="M48.5 34.8Q50.2 31.9 51.9 34.8M56.1 34.8Q57.8 31.9 59.5 34.8" fill="none" stroke="' + INK + '" stroke-width="1.7" stroke-linecap="round"/>',
    closed: '<path d="M48.5 33.6Q50.2 35.8 51.9 33.6M56.1 33.6Q57.8 35.8 59.5 33.6" fill="none" stroke="' + INK + '" stroke-width="1.7" stroke-linecap="round"/>',
    wink: '<ellipse cx="50.2" cy="34" rx="1.5" ry="1.9" fill="' + INK + '"/><path d="M56.1 34.8Q57.8 31.9 59.5 34.8" fill="none" stroke="' + INK + '" stroke-width="1.7" stroke-linecap="round"/>',
    up: '<g fill="' + INK + '"><ellipse cx="51" cy="32.9" rx="1.5" ry="1.9"/><ellipse cx="58.6" cy="32.9" rx="1.5" ry="1.9"/></g>'
  };
  var BROWS = {
    det: "M47.6 30.3l4.2 1.2M60.4 30.3l-4.2 1.2",
    up: "M47.8 29.9Q50 28.1 52.2 29.4M60.2 29.9Q58 28.1 55.8 29.4",
    soft: "M48 30.6Q50 29.8 52 30.4M60 30.6Q58 29.8 56 30.4",
    quirk: "M47.8 30.6Q50 30.2 52.1 30.9M60.3 29.2Q58 27.6 55.8 28.9",
    none: ""
  };
  var MOUTH = {
    grin: '<path d="M49.6 38.2Q54 42.4 58.4 38.2" fill="none" stroke="' + INK + '" stroke-width="1.7" stroke-linecap="round"/>',
    open: '<path d="M49.3 37.8Q54 45.6 58.7 37.8Z" fill="' + INK + '" stroke="' + INK + '" stroke-width="1" stroke-linejoin="round"/><path d="M51.6 41.4Q54 40 56.4 41.4Q54 43.2 51.6 41.4Z" fill="#F08FA8"/>',
    smile: '<path d="M51.2 38.7Q54 40.9 56.8 38.7" fill="none" stroke="' + INK + '" stroke-width="1.7" stroke-linecap="round"/>',
    o: '<ellipse cx="54" cy="39.6" rx="1.7" ry="2.1" fill="' + INK + '"/>',
    yawn: '<ellipse cx="54" cy="39.8" rx="2.5" ry="3.3" fill="' + INK + '"/>',
    hmm: '<path d="M51.4 39.4Q54 40.3 56.8 38.6" fill="none" stroke="' + INK + '" stroke-width="1.7" stroke-linecap="round"/>'
  };
  function face(f, hx, hy) {
    var b = BROWS[f.brows || "det"];
    return '<g transform="translate(' + hx + " " + hy + ')">' + (EYES[f.eyes || "open"] || "") +
      (b ? '<path d="' + b + '" fill="none" stroke="' + INK + '" stroke-width="1.7" stroke-linecap="round"/>' : "") +
      (MOUTH[f.mouth || "grin"] || "") + "</g>";
  }

  /* ---------- props: sil = shapes for the sticker border, art = the drawing ---------- */
  function outline(list, fill, w) { return list.map(function (s) { return prim(s, INK, w || 1.3) + prim(s, fill, 0); }).join(""); }
  var PROPS = {
    mug: function () {
      var cup = [["r", 45.5, 53.5, 17, 17, 3.6]];
      return { sil: cup, art: outline(cup, "#EFE7DA") + '<rect x="45.5" y="57" width="17" height="2.6" fill="#C9B9A2"/>' +
        '<path d="M51 51q-1.6-2 0-4t0-4M57 51q-1.6-2 0-4t0-4" fill="none" stroke="#FFFFFF" stroke-opacity=".85" stroke-width="1.5" stroke-linecap="round"/>' };
    },
    letter: function () {
      var env = [["r", 40.5, 56, 27, 18, 2.2]];
      return { sil: env, art: outline(env, "#F4EFE6") + '<path d="M41.3 57.2L54 66.2L66.7 57.2" fill="none" stroke="' + INK + '" stroke-width="1.3" stroke-linejoin="round"/><circle cx="54" cy="66" r="2.8" fill="var(--pk-seal,#34C47A)" stroke="' + INK + '" stroke-width="1.1"/>' };
    },
    book: function () {
      var pages = [["p", "M40 58Q47 55 54 58.5Q61 55 68 58V73Q61 70.5 54 73.5Q47 70.5 40 73Z"]];
      return { sil: pages, art: '<path d="M38.6 59.5V75Q47 72 54 75.6Q61 72 69.4 75V59.5" fill="#6B4A33" stroke="' + INK + '" stroke-width="1.3" stroke-linejoin="round"/>' + outline(pages, "#F4EFE6") +
        '<path d="M54 58.5V73.5M43 61.3q4-1.2 8 0M43 64.3q4-1.2 8 0M43 67.3q4-1.2 8 0M57 61.3q4-1.2 8 0M57 64.3q4-1.2 8 0M57 67.3q4-1.2 8 0" fill="none" stroke="' + INK + '" stroke-opacity=".45" stroke-width=".9" stroke-linecap="round"/>' };
    },
    phone: function () {
      var ph = [["r", 79.5, 13, 13.5, 21, 3, "rotate(10 86 23.5)"]];
      return { sil: ph, art: outline(ph, "#1F2624") + '<rect x="81.3" y="15" width="9.9" height="15.5" rx="1.8" fill="#3B4643" transform="rotate(10 86 23.5)"/><circle cx="87.3" cy="31.8" r=".9" fill="#8D9692" transform="rotate(10 86 23.5)"/>' };
    },
    dumbbell: function () {
      var plates = [["r", 64, 24.5, 6, 16, 2], ["r", 88, 24.5, 6, 16, 2]];
      return { sil: plates.concat([["s", 67, 32.5, 91, 32.5, 3.2]]), art: '<path d="M67 32.5H91" stroke="' + INK + '" stroke-width="4.8" stroke-linecap="round"/><path d="M67 32.5H91" stroke="#8D9692" stroke-width="2.2" stroke-linecap="round"/>' + outline(plates, "#2E3532") + '<path d="M66 27v6M90 27v6" stroke="#fff" stroke-opacity=".35" stroke-width="1.2" stroke-linecap="round"/>', behind: true };
    },
    sabzeh: function () {
      var pot = [["p", "M45 63H63L60.5 75H47.5Z"]];
      var blades = "M47.5 63.5Q46 56 43.5 52M50 63.5Q49.5 55 48.5 49.5M52.5 63.5Q53 54 52.5 47.5M55 63.5Q56 54.5 57.5 48.5M57.5 63.5Q59.5 56 62 51.5M60.5 63.5Q62.5 58.5 65 56";
      return { sil: pot.concat([["e", 54, 56, 10.5, 8]]), art: '<path d="' + blades + '" fill="none" stroke="' + INK + '" stroke-width="3.6" stroke-linecap="round"/><path d="' + blades + '" fill="none" stroke="#9BE36F" stroke-width="1.8" stroke-linecap="round"/>' + outline(pot, "#D9A066") +
        '<path d="M45.6 66.5H62.4" stroke="#F4EFE6" stroke-width="2"/>' };
    },
    bowl: function () {
      var g = [["c", 90, 74, 10.5]];
      return { sil: g, art: outline(g, "#CFEFFA") + '<path d="M80.4 72.5Q90 69.5 99.6 72.5A10.5 10.5 0 0 1 80.4 72.5Z" fill="#86CDEB"/><path d="M86 77.5q3-3.4 6.5 0q-3.5 3.4-6.5 0zM92.5 77.5l3-2.4v4.8z" fill="#F28B3C" stroke="' + INK + '" stroke-width=".9" stroke-linejoin="round"/><circle cx="88.4" cy="76.8" r=".6" fill="' + INK + '"/><path d="M83.5 68.5q2-2.5 5-3" fill="none" stroke="#fff" stroke-width="1.4" stroke-linecap="round"/>' };
    }
  };

  /* ---------- outfits (one per chapter, D82) ---------- */
  var OUTFITS = {
    none: null,
    band: function () {
      var band = [["p", "M43.3 30.6Q54 24.6 64.7 30.6L64.4 26.6Q54 20.6 43.6 26.6Z"]];
      return { sil: band, art: '<path d="M44.2 28.5L37.8 31.6M44.2 28.5L38.6 25.2" stroke="' + INK + '" stroke-width="4.2" stroke-linecap="round"/><path d="M44.2 28.5L37.8 31.6M44.2 28.5L38.6 25.2" stroke="#F7F8F7" stroke-width="2" stroke-linecap="round"/>' + outline(band, "#F7F8F7") + '<path d="M44 28.4Q54 22.6 64 28.4" fill="none" stroke="var(--pk-seal,#34C47A)" stroke-width="1.3"/>' };
    },
    winter: function () {
      var hat = [["p", "M43 31.5C42.6 17.5 65.4 17.5 65 31.5Z"]], brim = [["r", 42, 28, 24, 5.4, 2.6]], pom = [["c", 54, 18.8, 3.4]];
      var scarf = [["p", "M43.5 43Q54 49.5 64.5 43L65.6 47.6Q54 54.5 42.4 47.6Z"], ["p", "M56.5 48.5L59 62.5L64.2 61.4L61.4 47.6Z"]];
      return { sil: hat.concat(brim, pom, scarf),
        art: outline(scarf, "#EDE6D6") + '<path d="M57.6 52.5l4-.8M58.3 56.5l4-.8M43.6 45.6Q54 51.3 64.4 45.6" fill="none" stroke="' + INK + '" stroke-opacity=".45" stroke-width="1.2"/>' +
          outline(hat, "#EDE6D6") + outline(pom, "#EDE6D6") + outline(brim, "#D9CFBC") + '<path d="M47 24.5v3M50.5 22v5.5M54 21.3v6.2M57.5 22v5.5M61 24.5v3" stroke="' + INK + '" stroke-opacity=".3" stroke-width="1"/>' };
    },
    shades: function () {
      var l = [["r", 46.2, 31.6, 7.6, 5.2, 2.2]], r = [["r", 54.2, 31.6, 7.6, 5.2, 2.2]];
      return { sil: [], art: '<path d="M53.6 33.2h1M45.6 32.8l-1.6-.8M62.4 32.8l1.6-.8" stroke="' + INK + '" stroke-width="1.5" stroke-linecap="round"/>' + outline(l.concat(r), "#151918", .9) + '<path d="M47.6 33.2l2.2-1M55.6 33.2l2.2-1" stroke="#fff" stroke-opacity=".7" stroke-width="1" stroke-linecap="round"/>' };
    },
    sprout: function () {
      var leaves = [["p", "M54 24.5Q47 23.5 46 17.5Q52.5 17.5 54 24.5Z"], ["p", "M54 24.5Q60 22 61.5 15.5Q55 16.5 54 24.5Z"]];
      return { sil: leaves, art: '<path d="M54 25.5v-3" stroke="' + INK + '" stroke-width="2.4" stroke-linecap="round"/>' + outline(leaves, "#B6EE7E") };
    }
  };

  /* ---------- poses ---------- */
  var HIP_L = { e: [24.5, 62], h: [39.5, 71.5], hand: "fist", bi: .15 };
  var FLEX_L = { e: [25.5, 52.5], h: [29, 32], hand: "fist", bi: 1 };
  var FLEX_R = { e: [82.5, 52.5], h: [79, 32], hand: "fist", bi: 1 };
  var POSES = {
    flex:    { L: FLEX_L, R: FLEX_R, face: { eyes: "open", brows: "det", mouth: "grin" }, sparks: true },
    wave:    { L: HIP_L, R: { e: [85, 44], h: [87, 23.5], hand: "open", bi: .5 }, face: { eyes: "happy", brows: "up", mouth: "open" }, rot: -4 },
    cheer:   { L: { e: [26, 37], h: [21, 18.5], hand: "fist", bi: .55 }, R: { e: [82, 37], h: [87, 18.5], hand: "fist", bi: .55 }, face: { eyes: "happy", brows: "up", mouth: "open" }, sparks: true, rot: 0, hy: -1 },
    thumbs:  { L: HIP_L, R: { e: [86, 60], h: [82, 44], hand: "thumb", bi: .7 }, face: { eyes: "wink", brows: "quirk", mouth: "grin" }, rot: -6 },
    point:   { L: HIP_L, R: { e: [84, 61], h: [94, 71], hand: "point", bi: .35 }, face: { eyes: "open", brows: "up", mouth: "grin" }, rot: -6, hx: 1 },
    stretch: { L: { e: [27.5, 32], h: [46, 16.5], hand: "fist", bi: .5 }, R: { e: [80.5, 32], h: [62, 16.5], hand: "fist", bi: .5 }, face: { eyes: "closed", brows: "soft", mouth: "yawn" }, rot: 3, hy: 1 },
    tea:     { L: { e: [31, 73], h: [46, 70], hand: "fist", bi: .2, front: true }, R: { e: [77, 73], h: [62, 70], hand: "fist", bi: .2, front: true }, prop: "mug", face: { eyes: "happy", brows: "soft", mouth: "smile" }, rot: -3 },
    sleep:   { L: { e: [30, 67], h: [57, 67.5], hand: "fist", bi: .2, front: true }, R: { e: [78, 64], h: [51, 62.5], hand: "fist", bi: .2, front: true }, face: { eyes: "closed", brows: "soft", mouth: "smile" }, rot: 6, hx: 1.5, hy: 1, zzz: true },
    think:   { L: HIP_L, R: { e: [80, 67], h: [59.5, 49], hand: "fist", bi: .35, front: true }, face: { eyes: "up", brows: "quirk", mouth: "hmm" }, rot: -5, hx: -2, hy: -1.5 },
    selfie:  { L: FLEX_L, R: { e: [87, 52], h: [87, 36], hand: "fist", bi: .55, front: true }, prop: "phone", face: { eyes: "wink", brows: "det", mouth: "grin" }, rot: -6 },
    hug:     { L: { e: [22, 58], h: [12, 45], hand: "open", bi: .45 }, R: { e: [86, 58], h: [96, 45], hand: "open", bi: .45 }, face: { eyes: "happy", brows: "up", mouth: "open" }, rot: 0 },
    letter:  { L: { e: [29, 66], h: [42.5, 64.5], hand: "fist", bi: .2, front: true }, R: { e: [79, 66], h: [65.5, 64.5], hand: "fist", bi: .2, front: true }, prop: "letter", face: { eyes: "wide", brows: "up", mouth: "open" }, rot: -4 },
    read:    { L: { e: [29, 68], h: [41, 67], hand: "fist", bi: .2, front: true }, R: { e: [79, 68], h: [67, 67], hand: "fist", bi: .2, front: true }, prop: "book", face: { eyes: "closed", brows: "up", mouth: "smile" }, rot: -3, hy: 1 },
    wow:     { L: { e: [26, 64], h: [38.5, 44], hand: "open", bi: .4, front: true }, R: { e: [82, 64], h: [69.5, 44], hand: "open", bi: .4, front: true }, face: { eyes: "wide", brows: "up", mouth: "o" }, sparks: true, rot: 0 },
    curl:    { L: HIP_L, R: { e: [82.5, 52.5], h: [79, 32], hand: "fist", bi: 1 }, prop: "dumbbell", face: { eyes: "open", brows: "det", mouth: "grin" }, rot: -6 },
    sabzeh:  { L: { e: [29, 68], h: [45, 69], hand: "fist", bi: .2, front: true }, R: { e: [79, 68], h: [63, 69], hand: "fist", bi: .2, front: true }, prop: "sabzeh", face: { eyes: "happy", brows: "up", mouth: "open" }, rot: -4 },
    finish:  { L: FLEX_L, R: FLEX_R, prop: "bowl", face: { eyes: "happy", brows: "up", mouth: "open" }, sparks: true, rot: -6 }
  };

  function shines(list) { return '<g fill="#FFFFFF" opacity=".38">' + list.map(function (s) { return '<ellipse cx="' + n(s[0]) + '" cy="' + n(s[1]) + '" rx="' + (s[2] || 2.7) + '" ry="' + (s[3] || 1.6) + '" transform="rotate(-30 ' + n(s[0]) + " " + n(s[1]) + ')"/>'; }).join("") + "</g>"; }

  function svg(o) {
    o = o || {};
    var P = POSES[o.pose] || POSES.flex, col = o.color || "#34C47A";
    var hx = P.hx || 0, hy = P.hy || 0, rotDeg = P.rot == null ? -8 : P.rot;
    var aL = arm("L", P.L), aR = arm("R", P.R);
    var back = body(hx, hy), front = [];
    [[P.L, aL], [P.R, aR]].forEach(function (x) {
      back = back.concat(x[1].upper);
      if (x[0].front) front.push(x[1].lower); else back = back.concat(x[1].lower);
    });
    var prop = P.prop ? PROPS[P.prop]() : null;
    var outfit = o.outfit && OUTFITS[o.outfit] ? OUTFITS[o.outfit]() : null;
    var silAll = back.concat(prop ? prop.sil : [], outfit ? outfit.sil : []);
    front.forEach(function (f) { silAll = silAll.concat(f); });
    var bodyShine = [[49.5 + hx, 28.5 + hy, 3.6, 2.1], [44, 55, 3, 1.6]];
    [aL, aR].forEach(function (a) { bodyShine.push(a.shine); });
    var details = '<g fill="none" stroke="' + INK + '" stroke-opacity=".5" stroke-width="1.7" stroke-linecap="round"><path d="M45.5 58.5Q54 63.5 62.5 58.5"/><path d="M54 64.5v8"/><path d="M49.5 67q2 1.2 3 0M55.5 67q1 1.2 3 0"/></g>';
    var frontSvg = front.map(function (f) { return all(f, INK, OUTLINE) + all(f, col, 0); }).join("");
    var extras = "";
    if (P.sparks && o.sparks !== false) extras += star(38, 15, 3.4) + star(72, 13, 2.6) + star(94, 64, 2.2);
    if (P.zzz) extras += '<g fill="none" stroke="#FFFFFF" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M70 22h5l-5 6h5"/><path d="M79 12h7l-7 8h7"/></g>';
    var inner =
      '<g transform="translate(1.6 2.4)" opacity=".42">' + all(silAll, SHADOW, BORDER) + "</g>" +
      all(silAll, PAPER, BORDER) + (prop && prop.behind ? prop.art : "") +
      all(back, INK, OUTLINE) + all(back, col, 0) + shines(bodyShine) + details +
      face(P.face, hx, hy) +
      (prop && !prop.behind ? prop.art : "") + (outfit ? outfit.art : "") + frontSvg + extras;
    var vb = "-8 -2 124 102";
    var label = o.label ? ' role="img" aria-label="' + o.label + '"' : ' aria-hidden="true"';
    return '<svg class="pk" viewBox="' + vb + '"' + label + ' style="--pk-seal:' + col + '"><g transform="rotate(' + rotDeg + ' 54 54)">' + inner + "</g></svg>";
  }

  window.Piker = { svg: svg, POSES: Object.keys(POSES), OUTFITS: Object.keys(OUTFITS) };
})();
