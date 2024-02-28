import subprocess

subprocess.run("rm cache/*", shell=True)
subprocess.run("rm cache2/*", shell=True)

env_vars = {"LD_PRELOAD": "lib/lib440lib.so",
            "pin15440": "123456789",
            "proxyport15440": "4041"}

first_read_list = ["A", "B", "C"]

def slowread(filename):
    subprocess.run(f"./slowread {filename}", shell=True, env=env_vars, capture_output=False)

def normalread(filename_list):
    for filename in filename_list:
        subprocess.run(f"tools/440read {filename}", shell=True, env=env_vars, capture_output=False)

normalread(first_read_list)

second_read_list = ["B", "D", "E", "B"]
normalread(second_read_list)

third_read_list = ["F", "G"]

normalread(third_read_list)

env_vars_2 = {"LD_PRELOAD": "lib/lib440lib.so",
                "pin15440": "123456789",
                "proxyport15440": "4042",}

# modify A,F on another proxyport

def modify(filename_list):
    for filename in filename_list:
        input_data = "nmsl"
        proc = subprocess.Popen(f"tools/440write {filename}", shell=True, env=env_vars_2, stdin=subprocess.PIPE, stdout=subprocess.PIPE)
        proc.stdin.write(input_data.encode())
        proc.stdin.close()

modify(["A", "F"])

# read F A C
normalread(["F", "A", "C"])