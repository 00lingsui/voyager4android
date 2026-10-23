import json
import urllib.request

BASE_URL = "http://127.0.0.1:28888"

def get_status():
    try:
        with urllib.request.urlopen(f"{BASE_URL}/status") as resp:
            print("当前状态:", resp.read().decode())
    except Exception as e:
        print("连接失败，请确认是否执行了 'adb forward tcp:28888 tcp:28888':", e)

def send_toast(msg):
    req = urllib.request.Request(
        f"{BASE_URL}/action",
        data=json.dumps({"action": "toast", "text": msg}).encode(),
        headers={"Content-Type": "application/json"}
    )
    with urllib.request.urlopen(req) as resp:
        print("Toast 结果:", resp.read().decode())

def click_button(btn_text):
    req = urllib.request.Request(
        f"{BASE_URL}/action",
        data=json.dumps({"action": "click", "text": btn_text}).encode(),
        headers={"Content-Type": "application/json"}
    )
    with urllib.request.urlopen(req) as resp:
        print("点击结果:", resp.read().decode())

if __name__ == "__main__":
    print("1. 正在检查宿主状态...")
    get_status()
    print("2. 发送测试 Toast...")
    send_toast("Hello from Voyager Agent!")
