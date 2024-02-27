# write 1 megabyte of data to a file


file_list = ["A", "B", "C", "D", "E", "F", "G", "H"]
for file in file_list:
    with open(file, "wb") as f:
        f.write(b"0" * 1024 * 1024)