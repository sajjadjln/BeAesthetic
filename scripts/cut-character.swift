// cut <sheet.jpg> <col> <row> <out.jpg> <recolor 0|1>: one mood cell from a 3×2 sheet, 480 px, embers → cyan
import Foundation
import CoreGraphics
import ImageIO
import UniformTypeIdentifiers
let a = CommandLine.arguments
let src = CGImageSourceCreateWithURL(URL(fileURLWithPath: a[1]) as CFURL, nil)!
let img = CGImageSourceCreateImageAtIndex(src, 0, nil)!
let col = Int(a[2])!, row = Int(a[3])!, recolor = a[5] == "1"
let cw = img.width / 3, ch = img.height / 2, side = min(cw, ch) - 60
let rect = CGRect(x: col * cw + (cw - side) / 2, y: row * ch + 30, width: side, height: side)
let crop = img.cropping(to: rect)!
let n = 480
let ctx = CGContext(data: nil, width: n, height: n, bitsPerComponent: 8, bytesPerRow: n * 4, space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
ctx.interpolationQuality = .high
ctx.draw(crop, in: CGRect(x: 0, y: 0, width: n, height: n))
if recolor {
  let p = ctx.data!.bindMemory(to: UInt8.self, capacity: n * n * 4)
  for i in stride(from: 0, to: n * n * 4, by: 4) {
    let r = Int(p[i]), g = Int(p[i+1]), b = Int(p[i+2])
    if r > g + 15 && r > b + 10 && r > 50 { p[i] = UInt8(g >> 1); p[i+1] = UInt8(min(255, r + 60)); p[i+2] = UInt8(min(255, r + 80)) }
  }
}
let out = ctx.makeImage()!
let dest = CGImageDestinationCreateWithURL(URL(fileURLWithPath: a[4]) as CFURL, UTType.jpeg.identifier as CFString, 1, nil)!
CGImageDestinationAddImage(dest, out, [kCGImageDestinationLossyCompressionQuality: 0.82] as CFDictionary)
CGImageDestinationFinalize(dest)
