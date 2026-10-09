import AppKit
import CoreGraphics
import ImageIO
import Foundation

let root = URL(fileURLWithPath: CommandLine.arguments[1], isDirectory: true)
let captures = root.appendingPathComponent("captures", isDirectory: true)
let output = root.appendingPathComponent("comparisons", isDirectory: true)
try FileManager.default.createDirectory(at: output, withIntermediateDirectories: true)

struct Pair {
    let slug: String
    let title: String
    let stock: String
    let expressive: String
}

let pairs = [
    Pair(slug: "for-you", title: "For You", stock: "original-home.png", expressive: "modified-home.png"),
    Pair(slug: "home-actions", title: "Home card actions", stock: "original-home-actions.png", expressive: "modified-home-actions.png"),
    Pair(slug: "top-charts", title: "Top Charts", stock: "original-top-charts.png", expressive: "modified-top-charts.png"),
    Pair(slug: "categories", title: "Categories", stock: "original-categories.png", expressive: "modified-categories.png"),
    Pair(slug: "app-details", title: "App Details · Canva", stock: "original-details-canva.png", expressive: "modified-details-canva.png"),
    Pair(slug: "settings", title: "Settings", stock: "original-settings.png", expressive: "modified-settings.png"),
    Pair(slug: "customization", title: "Customization", stock: "original-customization.png", expressive: "modified-customization.png"),
]

func loadImage(_ name: String) throws -> CGImage {
    let url = captures.appendingPathComponent(name)
    guard let source = CGImageSourceCreateWithURL(url as CFURL, nil),
          let image = CGImageSourceCreateImageAtIndex(source, 0, nil) else {
        throw NSError(domain: "Showcase", code: 1, userInfo: [NSLocalizedDescriptionKey: "Could not load \(url.path)"])
    }
    return image
}

func drawText(_ text: String, at point: NSPoint, size: CGFloat, weight: NSFont.Weight, color: NSColor, in context: NSGraphicsContext) {
    let font = NSFont.systemFont(ofSize: size, weight: weight)
    let attributes: [NSAttributedString.Key: Any] = [.font: font, .foregroundColor: color]
    NSAttributedString(string: text, attributes: attributes).draw(at: point)
}

func writePNG(_ rep: NSBitmapImageRep, to url: URL) throws {
    guard let data = rep.representation(using: .png, properties: [:]) else {
        throw NSError(domain: "Showcase", code: 2, userInfo: [NSLocalizedDescriptionKey: "Could not encode \(url.path)"])
    }
    try data.write(to: url)
}

let canvasWidth: CGFloat = 1200
let margin: CGFloat = 32
let gap: CGFloat = 20
let labelHeight: CGFloat = 48
let titleHeight: CGFloat = 58
let footerHeight: CGFloat = 38
let imageWidth = (canvasWidth - (margin * 2) - gap) / 2
let sourceCropTop: CGFloat = 94
let sourceCropBottom: CGFloat = 54
let sourceWidth: CGFloat = 1080
let sourceHeight: CGFloat = 2412
let cropHeight = sourceHeight - sourceCropTop - sourceCropBottom
let imageHeight = imageWidth * cropHeight / sourceWidth
let canvasHeight = margin + titleHeight + labelHeight + imageHeight + footerHeight + margin

for pair in pairs {
    let stock = try loadImage(pair.stock)
    let expressive = try loadImage(pair.expressive)
    guard let stockCrop = stock.cropping(to: CGRect(x: 0, y: sourceCropTop, width: sourceWidth, height: cropHeight)),
          let expressiveCrop = expressive.cropping(to: CGRect(x: 0, y: sourceCropTop, width: sourceWidth, height: cropHeight)) else {
        throw NSError(domain: "Showcase", code: 3, userInfo: [NSLocalizedDescriptionKey: "Could not crop \(pair.title)"])
    }

    let rep = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: Int(canvasWidth), pixelsHigh: Int(canvasHeight), bitsPerSample: 8, samplesPerPixel: 4, hasAlpha: true, isPlanar: false, colorSpaceName: .deviceRGB, bytesPerRow: 0, bitsPerPixel: 0)!
    let context = NSGraphicsContext(bitmapImageRep: rep)!
    NSGraphicsContext.saveGraphicsState()
    NSGraphicsContext.current = context
    context.imageInterpolation = .high
    let bounds = CGRect(x: 0, y: 0, width: canvasWidth, height: canvasHeight)
    NSColor(calibratedRed: 0.965, green: 0.965, blue: 0.99, alpha: 1).setFill()
    bounds.fill()

    drawText(pair.title, at: NSPoint(x: margin, y: canvasHeight - margin - 35), size: 28, weight: .bold, color: NSColor(calibratedRed: 0.12, green: 0.15, blue: 0.25, alpha: 1), in: context)
    let labelY = canvasHeight - margin - titleHeight - 27
    drawText("STOCK · AURORA STORE 4.8.4", at: NSPoint(x: margin, y: labelY), size: 16, weight: .semibold, color: NSColor(calibratedRed: 0.28, green: 0.30, blue: 0.38, alpha: 1), in: context)
    drawText("MATERIAL 3 EXPRESSIVE · PREVIEW", at: NSPoint(x: margin + imageWidth + gap, y: labelY), size: 16, weight: .semibold, color: NSColor(calibratedRed: 0.18, green: 0.28, blue: 0.58, alpha: 1), in: context)

    let imageY = margin + footerHeight
    let leftRect = CGRect(x: margin, y: imageY, width: imageWidth, height: imageHeight)
    let rightRect = CGRect(x: margin + imageWidth + gap, y: imageY, width: imageWidth, height: imageHeight)
    for rect in [leftRect, rightRect] {
        NSColor.white.setFill()
        NSBezierPath(roundedRect: rect, xRadius: 18, yRadius: 18).fill()
    }
    NSGraphicsContext.saveGraphicsState()
    NSBezierPath(roundedRect: leftRect, xRadius: 18, yRadius: 18).addClip()
    NSImage(cgImage: stockCrop, size: NSSize(width: sourceWidth, height: cropHeight)).draw(in: leftRect)
    NSGraphicsContext.restoreGraphicsState()
    NSGraphicsContext.saveGraphicsState()
    NSBezierPath(roundedRect: rightRect, xRadius: 18, yRadius: 18).addClip()
    NSImage(cgImage: expressiveCrop, size: NSSize(width: sourceWidth, height: cropHeight)).draw(in: rightRect)
    NSGraphicsContext.restoreGraphicsState()

    drawText("Captured on the same phone · live catalog data can vary", at: NSPoint(x: margin, y: margin - 1), size: 13, weight: .regular, color: NSColor(calibratedRed: 0.36, green: 0.38, blue: 0.45, alpha: 1), in: context)
    NSGraphicsContext.restoreGraphicsState()

    try writePNG(rep, to: output.appendingPathComponent("comparison-\(pair.slug).png"))
}

// A standalone feature card for the redesigned full-screen search flow.
let search = try loadImage("modified-search-results.png")
guard let searchCrop = search.cropping(to: CGRect(x: 0, y: sourceCropTop, width: sourceWidth, height: sourceHeight - sourceCropTop - sourceCropBottom)) else {
    throw NSError(domain: "Showcase", code: 4, userInfo: [NSLocalizedDescriptionKey: "Could not crop search screenshot"])
}
let searchWidth: CGFloat = 640
let searchHeight = searchWidth * cropHeight / sourceWidth
let searchCanvasWidth: CGFloat = 720
let searchCanvasHeight = searchHeight + 142
let searchRep = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: Int(searchCanvasWidth), pixelsHigh: Int(searchCanvasHeight), bitsPerSample: 8, samplesPerPixel: 4, hasAlpha: true, isPlanar: false, colorSpaceName: .deviceRGB, bytesPerRow: 0, bitsPerPixel: 0)!
let searchContext = NSGraphicsContext(bitmapImageRep: searchRep)!
NSGraphicsContext.saveGraphicsState()
NSGraphicsContext.current = searchContext
NSColor(calibratedRed: 0.965, green: 0.965, blue: 0.99, alpha: 1).setFill()
CGRect(x: 0, y: 0, width: searchCanvasWidth, height: searchCanvasHeight).fill()
drawText("Full-screen Material 3 Search", at: NSPoint(x: 34, y: searchCanvasHeight - 48), size: 25, weight: .bold, color: NSColor(calibratedRed: 0.12, green: 0.15, blue: 0.25, alpha: 1), in: searchContext)
drawText("Centered search field · suggestions · list results", at: NSPoint(x: 34, y: searchCanvasHeight - 82), size: 14, weight: .regular, color: NSColor(calibratedRed: 0.36, green: 0.38, blue: 0.45, alpha: 1), in: searchContext)
let searchRect = CGRect(x: 40, y: 24, width: searchWidth, height: searchHeight)
NSGraphicsContext.saveGraphicsState()
NSBezierPath(roundedRect: searchRect, xRadius: 18, yRadius: 18).addClip()
NSImage(cgImage: searchCrop, size: NSSize(width: sourceWidth, height: cropHeight)).draw(in: searchRect)
NSGraphicsContext.restoreGraphicsState()
NSGraphicsContext.restoreGraphicsState()
try writePNG(searchRep, to: output.appendingPathComponent("feature-search.png"))
