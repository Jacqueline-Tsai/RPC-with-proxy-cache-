# RPC-with-proxy-cache-


# Remote File Operations over Java RMI

## Overview
This project lets applications perform **transparent, POSIX‑style file operations** (`open`, `read`, `write`, `close`, etc.) on a remote machine.  
A Java **proxy** process intercepts the calls, caches file blocks locally, and forwards the operations to a remote **RMI server** that owns the authoritative data.

Key features  

* Java RMI transport — no custom socket protocol to maintain  
* **Write‑back, LRU cache** with pinning, versioning, and clone‑on‑write  
* Chunked transfers (500 kB) for large files  
* Works on any 64‑bit OS with a Java 8+ runtime (tested on Linux)

---

## Project Structure
```yaml
.
├── README.md
├── docs/
│   └── design.pdf # Detailed design & sequence diagrams
├── lib/… # pre‑built .class helpers + JNI stub
├── src/
│   ├── FileCache.java # metadata + clone‑on‑write logic
│   ├── LRUCache.java # size‑bounded, pin‑aware LRU cache
│   ├── Proxy.java # client‑side proxy + RPC receiver
│   ├── RmiInterface.java # Remote interface (extends java.rmi.Remote)
│   ├── Server.java # remote file server
│   └── Makefile # build script
├── tests/… # testing bash files
└── tools/… # 440cat, 440read, 440write, 440rm
```

---

## Requirements

### System Requirements
* 64‑bit Linux / macOS / Windows
* Java 8 (JDK 1.8) or newer
* TCP connectivity between proxy and server hosts

### Build Dependencies
* GNU `make` (or invoke `javac` / your own build system)
* (Optional) `jar` if you want a single runnable JAR

---

## Build Instructions
```bash
# Compile everything under ./src
make
```

### Artifacts
```
build/server/Server.class – remote server
build/proxy/Proxy.class – client‑side proxy
```

### Usage
1 · Start the server
```bash
java -cp build Server <rmi_registry_port> <root_dir>
```
2 · Launch a proxy on each client
```bash
java -cp build Proxy <server_ip_address> <rmi_registry_port> <local_cache_directory> <cache_size>
```
3 · Make POSIX‑style calls through the proxy
Proxy spins up an RPC receiver that services the FileHandling API.
```java
FileHandling fh = RPCreceiver.get();          // returns FileHandling stub
int fd   = fh.open("/docs/report.pdf", OpenOption.READ);
byte[] b = new byte[4096];
fh.read(fd, b);                               // transparent caching
fh.close(fd);
```

## How it Works
| Component     | Responsibilities |
| ------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Proxy**     | • Registers with the RPC receiver<br>• Maintains an `LRUCache` mapping *path → FileCache*<br>• Fetches / writes file blocks via `RmiInterface`<br>• Clone‑on‑write for concurrent writers |
| **FileCache** | • Immutable *base* copy is **read‑only & pinned**<br>• `clone()` creates a writable shadow (id > 0)<br>• Tracks size, version, modification flag                                          |
| **Server**    | • Stores files under a *root directory*<br>• Uses Java NIO/`RandomAccessFile` for chunk I/O<br>• Maintains per‑file version counters (`AtomicInteger`) |
| **Transport** | Java RMI (binary, TCP) – no custom marshalling code |


### Data path on a write:
1. write() hits a writable FileCache; cache size updated.
2. On close(), modified file is streamed back in 500 kB chunks via putFile(); server bumps the version.
3. Proxy flips the cache entry back to a read‑only base file.

### Configuration Options
| Flag / Env Var            | Default    | Description                       |
| ------------------------- | ---------- | --------------------------------- |
| `MAX_CHUNK_SIZE` (const)  |  500 000 B | Wire chunk size for file transfer |
| `Proxy` arg `<cachesize>` | required   | Total cache budget (bytes)        |
| `Server` arg `<rootdir>`  |  `./`      | Directory exposed to clients      |


## Documentation
See docs/design.pdf for design docs.

