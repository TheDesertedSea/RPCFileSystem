import sys

def read_and_print_file_as_ascii(file_path):
    try:
        with open(file_path, 'rb') as file:
            byte = file.read(1)
            while byte:
                # Convert byte to ASCII character and print
                print(byte, end=' ')
                byte = file.read(1)
    except Exception as e:
        print("An error occurred:", e)


if len(sys.argv) != 2:
    print("Usage: python script.py <file_path>")
else:
    file_path = sys.argv[1]
    read_and_print_file_as_ascii(file_path)