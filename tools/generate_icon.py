#!/usr/bin/env python3
import os
import struct
import zlib


DENSITIES = {
    "ldpi": 36,
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
}

MODULES = 21


def finder(grid, ox, oy):
    for y in range(5):
        for x in range(5):
            border = x in (0, 4) or y in (0, 4)
            center = 1 <= x <= 3 and 1 <= y <= 3
            grid[oy + y][ox + x] = border or center
    grid[oy + 2][ox + 2] = False


def draw_letter_q(grid, ox, oy):
    pattern = [
        "01110",
        "10001",
        "10001",
        "10001",
        "10101",
        "10010",
        "01101",
    ]
    draw_pattern(grid, ox, oy, pattern)


def draw_letter_r(grid, ox, oy):
    pattern = [
        "11110",
        "10001",
        "10001",
        "11110",
        "10100",
        "10010",
        "10001",
    ]
    draw_pattern(grid, ox, oy, pattern)


def draw_pattern(grid, ox, oy, pattern):
    for y, row in enumerate(pattern):
        for x, value in enumerate(row):
            if value == "1":
                grid[oy + y][ox + x] = True


def make_grid():
    grid = [[False for _ in range(MODULES)] for _ in range(MODULES)]
    finder(grid, 0, 0)
    finder(grid, 16, 0)
    finder(grid, 0, 16)
    draw_letter_q(grid, 6, 7)
    draw_letter_r(grid, 12, 7)
    for x, y in [
        (7, 1), (10, 2), (13, 4), (6, 5), (19, 6),
        (5, 15), (8, 17), (11, 15), (14, 18), (19, 19),
        (17, 11), (19, 13), (6, 14),
    ]:
        grid[y][x] = True
    return grid


def render(size):
    grid = make_grid()
    pixels = [[0 for _ in range(size)] for _ in range(size)]
    for py in range(size):
        gy = py * MODULES // size
        for px in range(size):
            gx = px * MODULES // size
            pixels[py][px] = 0 if grid[gy][gx] else 1
    return pixels


def png_chunk(kind, data):
    return (
        struct.pack(">I", len(data))
        + kind
        + data
        + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)
    )


def write_png(path, pixels):
    height = len(pixels)
    width = len(pixels[0])
    rows = bytearray()
    row_bytes = (width + 7) // 8
    for row in pixels:
        rows.append(0)
        packed = bytearray(row_bytes)
        for x, value in enumerate(row):
            if value:
                packed[x // 8] |= 1 << (7 - (x % 8))
        rows.extend(packed)

    header = struct.pack(">IIBBBBB", width, height, 1, 0, 0, 0, 0)
    data = (
        b"\x89PNG\r\n\x1a\n"
        + png_chunk(b"IHDR", header)
        + png_chunk(b"IDAT", zlib.compress(bytes(rows), 9))
        + png_chunk(b"IEND", b"")
    )
    with open(path, "wb") as fh:
        fh.write(data)


def main():
    for density, size in DENSITIES.items():
        directory = os.path.join("res", "drawable-" + density)
        os.makedirs(directory, exist_ok=True)
        write_png(os.path.join(directory, "ic_launcher.png"), render(size))


if __name__ == "__main__":
    main()
