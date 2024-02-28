import subprocess
import threading

subprocess.run("rm cache/*", shell=True)
subprocess.run("rm cache2/*", shell=True)

env_vars = {"LD_PRELOAD": "lib/lib440lib.so",
            "pin15440": "123456789",
            "proxyport15440": "4041"}

# use multiple threads

def slowread(filename):
    subprocess.run(f"./slowread {filename}", shell=True, env=env_vars, capture_output=False)

def normalread(filename_list):
    for filename in filename_list:
        subprocess.run(f"tools/440read {filename}", shell=True, env=env_vars, capture_output=False)

normal_file_list = ["B", "C", "D", "E", "F", "G", "H"]

thread_slows = [threading.Thread(target=slowread, args=("A",)) for _ in range(4)]
# sleep for 0.5 second to make sure slowread is ready
thread_first = threading.Thread(target=normalread, args=(["A"],))

thread_normal = threading.Thread(target=normalread, args=(normal_file_list,))

for thread_slow in thread_slows:
    thread_slow.start()
thread_first.start()
thread_first.join()

thread_normal.start()
thread_normal.join()

# now start slowrite
print("Starting slowrite")
def slowrite(filename):
    subprocess.run(f"./slowrite {filename} 2", shell=True, env=env_vars, capture_output=False)

slow_write_list = ["G", "H"]
thread_slow_writes = [threading.Thread(target=slowrite, args=(filename,)) for filename in slow_write_list]

for thread_slow_write in thread_slow_writes:
    thread_slow_write.start()

for thread_slow_write in thread_slow_writes:
    thread_slow_write.join()

gh_list = ["G", "H"]
thread_normal_gh = threading.Thread(target=normalread, args=(gh_list,))
thread_normal_gh.start()
thread_normal_gh.join()

for thread_slow in thread_slows:
    thread_slow.join()

threading.Thread(target=normalread, args=(["C", "D", "E"],)).start()

