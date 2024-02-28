with open("large_file", "wb") as f:
    f.write(b"0" * 1024 * 1024 * 10)