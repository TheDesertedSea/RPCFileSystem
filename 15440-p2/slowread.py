import subprocess
import threading

env_vars = {"LD_PRELOAD": "lib/lib440lib.so",
            "pin15440": "123456789",
            "proxyport15440": "4041"}

# use multiple threads

def slowread(filename):
    subprocess.run(f"./myread {filename}", shell=True, env=env_vars, capture_output=False)

def normalread(filename_list):
    for filename in filename_list:
        subprocess.run(f"tools/440read {filename}", shell=True, env=env_vars, capture_output=False)

normal_file_list = ["B", "C", "D", "E", "F", "G", "H"]

thread_slow = threading.Thread(target=slowread, args=("A",))
thread_normal = threading.Thread(target=normalread, args=(normal_file_list,))

thread_slow.start()
thread_normal.start()