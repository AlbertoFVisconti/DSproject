# Reliable Queueing System (RQS)

Project for the **Distributed Systems** course (AY 2024–2025).  
Authors: **Roberto Petenzi** and **Alberto Visconti**

---

## 📌 Goal
Build a reliable and fault-tolerant system where brokers collaborate to provide distributed queues to clients.

---

## ⚙️ Requirements
- Queues are **append-only**, **FIFO**, and **replicated** across brokers.  
- Clients can:
  - Create new queues
  - Append values
  - Read values from queues  
- Brokers automatically track which value each client should read.  

**Assumptions:**
- Broker storage is reliable.  
- No network partitions.  
- Brokers may fail (crash failures).  

---

## 🛠️ Solution
- Implemented in **Java** using **TCP sockets**.  
- Brokers are divided into:
  - **Leader** → handles client requests.
  - **Followers** → replicate updates and serve client requests.  
- **Leader election**: if the leader fails, the follower with the most entries becomes the new leader.  
- **State synchronization**: when a new peer joins mid-execution, the leader updates it.  
- **Client deduplication**: clients discard duplicate messages via message UUIDs.  

---

## 🔄 Operations

### Add
1. Client sends a message to the leader.  
2. Leader broadcasts it to all peers.  
3. Probability of failure:  
   ```
   err_rate * (1 + (n - 1)(1 - err_rate))
   ```

### Read
1. Client sends read request to the leader.  
2. Leader broadcasts request to all peers.  
3. All peers reply.  
4. Client discards duplicates and continues receiving messages as long as one peer is alive.  

---

## 🧪 Testing
- **Mininet** is used to simulate the system.  
- Adjustable parameters:
  - Link latency
  - Error rate  
- Tested with **2 clients** and **3 brokers**.  

---

## 🚀 Running the Emulator

Inside the **`networkEmulator`**:

```
Usage:
- To display help: use the [-h] or [--help] flags
- To run the emulator: pass the number of hosts as an argument
```

Description:
- Starts a Mininet network with:
  - A central switch  
  - N routers connected to the switch  
  - Each router connected to a single host  

### Commands
- Run emulator (requires root):
  ```bash
  sudo venv/bin/python networkEmulator.py N
  ```
- Install dependencies:
  ```bash
  pip install -r requirements.txt
  ```
- Change **error rate** and **delay** by editing the global variables in `networkEmulator.py`.  

---

## 📡 Debugging & Monitoring
You can run **Wireshark** in the terminal with filters such as:
```bash
tcp.port == 5000
ip.src == 127.0.0.1
```

---

## ⚒️ Customizing Build Execution
To run different compiled files, update the `maketerm` command in `networkEmulator`:

```python
terminals.append(
    makeTerm(
        host,
        cmd=f"bash -c 'cd ../out/production/progetto1 && java peer.Peer; exec bash'",
    )
)
```
