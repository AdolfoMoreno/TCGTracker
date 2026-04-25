#!/usr/bin/swift

import AppKit
import Foundation
import SQLite3

struct CardRecord {
    let id: String
    let name: String
    let number: String
    let rarity: String
    let types: String
    let supertype: String
    let setName: String
    let series: String
    let imageSmallPath: String
    let imageLargePath: String
}

struct RenderSpec {
    let size: NSSize
    let imagePath: KeyPath<CardRecord, String>
}

enum ScriptError: Error, CustomStringConvertible {
    case databaseOpenFailed(String)
    case queryPrepareFailed(String)
    case writeFailed(String)

    var description: String {
        switch self {
        case .databaseOpenFailed(let message): return message
        case .queryPrepareFailed(let message): return message
        case .writeFailed(let message): return message
        }
    }
}

let rootURL = URL(fileURLWithPath: FileManager.default.currentDirectoryPath)
let assetsURL = rootURL.appendingPathComponent("src/main/assets", isDirectory: true)
let databaseURL = assetsURL.appendingPathComponent("database/pokemon_tcg_tracker.db")

let renderSpecs = [
    RenderSpec(size: NSSize(width: 180, height: 252), imagePath: \.imageSmallPath),
    RenderSpec(size: NSSize(width: 420, height: 586), imagePath: \.imageLargePath),
]

func queryCards() throws -> [CardRecord] {
    var database: OpaquePointer?
    guard sqlite3_open(databaseURL.path, &database) == SQLITE_OK else {
        let message = String(cString: sqlite3_errmsg(database))
        sqlite3_close(database)
        throw ScriptError.databaseOpenFailed("Could not open \(databaseURL.path): \(message)")
    }
    defer { sqlite3_close(database) }

    let query = """
        SELECT c.id, c.name, c.number, c.rarity, c.types, c.supertype,
               s.name, s.series, c.imageSmall, c.imageLarge
        FROM cards c
        INNER JOIN sets s ON s.id = c.setId
        ORDER BY s.releaseDate DESC,
                 CASE WHEN c.number GLOB '[0-9]*' THEN CAST(c.number AS INTEGER) ELSE 999999 END,
                 c.number ASC,
                 c.id ASC
    """

    var statement: OpaquePointer?
    guard sqlite3_prepare_v2(database, query, -1, &statement, nil) == SQLITE_OK else {
        let message = String(cString: sqlite3_errmsg(database))
        throw ScriptError.queryPrepareFailed("Could not prepare card query: \(message)")
    }
    defer { sqlite3_finalize(statement) }

    func stringColumn(_ index: Int32) -> String {
        guard let text = sqlite3_column_text(statement, index) else { return "" }
        return String(cString: text)
    }

    var cards: [CardRecord] = []
    while sqlite3_step(statement) == SQLITE_ROW {
        cards.append(
            CardRecord(
                id: stringColumn(0),
                name: stringColumn(1),
                number: stringColumn(2),
                rarity: stringColumn(3),
                types: stringColumn(4),
                supertype: stringColumn(5),
                setName: stringColumn(6),
                series: stringColumn(7),
                imageSmallPath: stringColumn(8),
                imageLargePath: stringColumn(9)
            )
        )
    }
    return cards
}

func primaryType(for card: CardRecord) -> String {
    let trimmed = card.types
        .split(separator: ",")
        .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
        .first
    return trimmed?.isEmpty == false ? trimmed! : "Colorless"
}

func typeColors(for type: String) -> (NSColor, NSColor, NSColor) {
    switch type {
    case "Fire":
        return (NSColor(calibratedRed: 0.95, green: 0.46, blue: 0.28, alpha: 1), NSColor(calibratedRed: 0.62, green: 0.16, blue: 0.09, alpha: 1), NSColor(calibratedRed: 1.00, green: 0.89, blue: 0.76, alpha: 1))
    case "Water":
        return (NSColor(calibratedRed: 0.26, green: 0.57, blue: 0.90, alpha: 1), NSColor(calibratedRed: 0.08, green: 0.24, blue: 0.56, alpha: 1), NSColor(calibratedRed: 0.85, green: 0.93, blue: 1.00, alpha: 1))
    case "Grass":
        return (NSColor(calibratedRed: 0.32, green: 0.70, blue: 0.39, alpha: 1), NSColor(calibratedRed: 0.11, green: 0.38, blue: 0.19, alpha: 1), NSColor(calibratedRed: 0.87, green: 0.96, blue: 0.84, alpha: 1))
    case "Lightning":
        return (NSColor(calibratedRed: 0.99, green: 0.80, blue: 0.23, alpha: 1), NSColor(calibratedRed: 0.68, green: 0.47, blue: 0.02, alpha: 1), NSColor(calibratedRed: 1.00, green: 0.96, blue: 0.81, alpha: 1))
    case "Psychic":
        return (NSColor(calibratedRed: 0.72, green: 0.44, blue: 0.84, alpha: 1), NSColor(calibratedRed: 0.37, green: 0.15, blue: 0.52, alpha: 1), NSColor(calibratedRed: 0.94, green: 0.87, blue: 1.00, alpha: 1))
    case "Fighting":
        return (NSColor(calibratedRed: 0.83, green: 0.48, blue: 0.27, alpha: 1), NSColor(calibratedRed: 0.50, green: 0.24, blue: 0.12, alpha: 1), NSColor(calibratedRed: 0.98, green: 0.89, blue: 0.81, alpha: 1))
    case "Darkness":
        return (NSColor(calibratedRed: 0.32, green: 0.34, blue: 0.42, alpha: 1), NSColor(calibratedRed: 0.12, green: 0.13, blue: 0.18, alpha: 1), NSColor(calibratedRed: 0.86, green: 0.87, blue: 0.92, alpha: 1))
    case "Metal":
        return (NSColor(calibratedRed: 0.67, green: 0.72, blue: 0.77, alpha: 1), NSColor(calibratedRed: 0.34, green: 0.39, blue: 0.45, alpha: 1), NSColor(calibratedRed: 0.94, green: 0.96, blue: 0.99, alpha: 1))
    case "Dragon":
        return (NSColor(calibratedRed: 0.52, green: 0.51, blue: 0.94, alpha: 1), NSColor(calibratedRed: 0.23, green: 0.18, blue: 0.55, alpha: 1), NSColor(calibratedRed: 0.90, green: 0.90, blue: 1.00, alpha: 1))
    default:
        return (NSColor(calibratedRed: 0.82, green: 0.74, blue: 0.58, alpha: 1), NSColor(calibratedRed: 0.50, green: 0.42, blue: 0.27, alpha: 1), NSColor(calibratedRed: 0.97, green: 0.94, blue: 0.87, alpha: 1))
    }
}

func rarityAccent(for rarity: String) -> NSColor {
    if rarity.contains("Secret") { return NSColor(calibratedRed: 0.95, green: 0.74, blue: 0.23, alpha: 1) }
    if rarity.contains("Ultra") || rarity.contains("VMAX") { return NSColor(calibratedRed: 0.91, green: 0.44, blue: 0.62, alpha: 1) }
    if rarity.contains("Holo") { return NSColor(calibratedRed: 0.40, green: 0.83, blue: 0.90, alpha: 1) }
    if rarity.contains("Rare") { return NSColor(calibratedRed: 0.47, green: 0.58, blue: 0.92, alpha: 1) }
    if rarity.contains("Uncommon") { return NSColor(calibratedRed: 0.43, green: 0.69, blue: 0.46, alpha: 1) }
    return NSColor(calibratedRed: 0.62, green: 0.58, blue: 0.49, alpha: 1)
}

func textAttributes(size: CGFloat, weight: NSFont.Weight, color: NSColor) -> [NSAttributedString.Key: Any] {
    [
        .font: NSFont.systemFont(ofSize: size, weight: weight),
        .foregroundColor: color
    ]
}

func fit(_ text: String, maxLength: Int) -> String {
    if text.count <= maxLength { return text }
    return String(text.prefix(maxLength - 1)) + "…"
}

func drawCard(_ card: CardRecord, spec: RenderSpec) -> NSImage {
    let image = NSImage(size: spec.size)
    image.lockFocus()
    defer { image.unlockFocus() }

    guard let context = NSGraphicsContext.current?.cgContext else { return image }
    context.setShouldAntialias(true)
    context.setAllowsAntialiasing(true)

    let scale = spec.size.width / 180.0
    let outerRect = NSRect(origin: .zero, size: spec.size)
    let cardRect = outerRect.insetBy(dx: 6 * scale, dy: 6 * scale)
    let primaryType = primaryType(for: card)
    let (baseColor, darkColor, paleColor) = typeColors(for: primaryType)
    let accentColor = rarityAccent(for: card.rarity)

    NSColor(calibratedWhite: 0.94, alpha: 1).setFill()
    outerRect.fill()

    let shadow = NSShadow()
    shadow.shadowBlurRadius = 10 * scale
    shadow.shadowOffset = NSSize(width: 0, height: -2 * scale)
    shadow.shadowColor = NSColor(calibratedWhite: 0, alpha: 0.12)
    shadow.set()

    let cardPath = NSBezierPath(roundedRect: cardRect, xRadius: 16 * scale, yRadius: 16 * scale)
    NSColor.white.setFill()
    cardPath.fill()
    shadow.shadowColor = nil

    let insetRect = cardRect.insetBy(dx: 10 * scale, dy: 10 * scale)
    let framePath = NSBezierPath(roundedRect: insetRect, xRadius: 14 * scale, yRadius: 14 * scale)
    let gradient = NSGradient(starting: baseColor, ending: darkColor)
    gradient?.draw(in: framePath, angle: 270)

    let topBandRect = NSRect(
        x: insetRect.minX + 10 * scale,
        y: insetRect.maxY - 58 * scale,
        width: insetRect.width - 20 * scale,
        height: 42 * scale
    )
    let topBand = NSBezierPath(roundedRect: topBandRect, xRadius: 10 * scale, yRadius: 10 * scale)
    NSColor.white.withAlphaComponent(0.18).setFill()
    topBand.fill()

    let artRect = NSRect(
        x: insetRect.minX + 12 * scale,
        y: insetRect.minY + 118 * scale,
        width: insetRect.width - 24 * scale,
        height: insetRect.height - 192 * scale
    )
    let artPath = NSBezierPath(roundedRect: artRect, xRadius: 16 * scale, yRadius: 16 * scale)
    let artGradient = NSGradient(starting: paleColor, ending: baseColor.withAlphaComponent(0.78))
    artGradient?.draw(in: artPath, angle: 315)

    NSColor.white.withAlphaComponent(0.34).setFill()
    NSBezierPath(ovalIn: NSRect(
        x: artRect.midX - (artRect.width * 0.28),
        y: artRect.midY - (artRect.width * 0.28),
        width: artRect.width * 0.56,
        height: artRect.width * 0.56
    )).fill()

    NSColor.white.withAlphaComponent(0.2).setFill()
    NSBezierPath(ovalIn: NSRect(
        x: artRect.minX + 18 * scale,
        y: artRect.maxY - 82 * scale,
        width: 70 * scale,
        height: 70 * scale
    )).fill()

    let footerRect = NSRect(
        x: insetRect.minX + 12 * scale,
        y: insetRect.minY + 12 * scale,
        width: insetRect.width - 24 * scale,
        height: 94 * scale
    )
    let footerPath = NSBezierPath(roundedRect: footerRect, xRadius: 12 * scale, yRadius: 12 * scale)
    NSColor.white.withAlphaComponent(0.92).setFill()
    footerPath.fill()

    let accentRect = NSRect(
        x: footerRect.minX,
        y: footerRect.maxY - 10 * scale,
        width: footerRect.width,
        height: 10 * scale
    )
    let accentPath = NSBezierPath(roundedRect: accentRect, xRadius: 8 * scale, yRadius: 8 * scale)
    accentColor.setFill()
    accentPath.fill()

    let seriesText = fit(card.series.uppercased(), maxLength: scale > 1.5 ? 30 : 22)
    let nameText = fit(card.name, maxLength: scale > 1.5 ? 26 : 16)
    let setText = fit(card.setName, maxLength: scale > 1.5 ? 30 : 18)
    let typeText = fit(primaryType, maxLength: 12)
    let rarityText = fit(card.rarity, maxLength: scale > 1.5 ? 22 : 16)
    let supertypeText = fit(card.supertype, maxLength: 14)

    NSString(string: seriesText).draw(
        at: NSPoint(x: topBandRect.minX + 12 * scale, y: topBandRect.minY + 12 * scale),
        withAttributes: textAttributes(size: 10 * scale, weight: .semibold, color: .white)
    )
    NSString(string: "#\(card.number)").draw(
        at: NSPoint(x: topBandRect.maxX - 52 * scale, y: topBandRect.minY + 11 * scale),
        withAttributes: textAttributes(size: 11 * scale, weight: .bold, color: .white)
    )
    NSString(string: nameText).draw(
        in: NSRect(
            x: artRect.minX + 18 * scale,
            y: artRect.maxY - 64 * scale,
            width: artRect.width - 36 * scale,
            height: 52 * scale
        ),
        withAttributes: textAttributes(size: 18 * scale, weight: .bold, color: darkColor)
    )
    NSString(string: typeText).draw(
        at: NSPoint(x: artRect.minX + 20 * scale, y: artRect.minY + 16 * scale),
        withAttributes: textAttributes(size: 12 * scale, weight: .semibold, color: darkColor)
    )
    NSString(string: setText).draw(
        at: NSPoint(x: footerRect.minX + 14 * scale, y: footerRect.minY + 54 * scale),
        withAttributes: textAttributes(size: 13 * scale, weight: .bold, color: darkColor)
    )
    NSString(string: rarityText).draw(
        at: NSPoint(x: footerRect.minX + 14 * scale, y: footerRect.minY + 32 * scale),
        withAttributes: textAttributes(size: 11 * scale, weight: .semibold, color: accentColor)
    )
    NSString(string: supertypeText).draw(
        at: NSPoint(x: footerRect.minX + 14 * scale, y: footerRect.minY + 14 * scale),
        withAttributes: textAttributes(size: 11 * scale, weight: .regular, color: NSColor(calibratedWhite: 0.28, alpha: 1))
    )

    return image
}

func savePNG(_ image: NSImage, to url: URL) throws {
    guard
        let tiffData = image.tiffRepresentation,
        let bitmap = NSBitmapImageRep(data: tiffData),
        let pngData = bitmap.representation(using: .png, properties: [:])
    else {
        throw ScriptError.writeFailed("Could not convert image for \(url.path)")
    }

    try FileManager.default.createDirectory(
        at: url.deletingLastPathComponent(),
        withIntermediateDirectories: true,
        attributes: nil
    )
    try pngData.write(to: url)
}

let cards = try queryCards()
print("Generating card scan assets for \(cards.count) cards...")

for (index, card) in cards.enumerated() {
    autoreleasepool {
        for spec in renderSpecs {
            let outputURL = assetsURL.appendingPathComponent(card[keyPath: spec.imagePath])
            let image = drawCard(card, spec: spec)
            do {
                try savePNG(image, to: outputURL)
            } catch {
                fputs("Failed to write \(outputURL.path): \(error)\n", stderr)
                exit(1)
            }
        }
    }

    if (index + 1) % 500 == 0 || index + 1 == cards.count {
        print("Rendered \(index + 1)/\(cards.count) cards")
    }
}

print("Card scan asset generation complete.")
