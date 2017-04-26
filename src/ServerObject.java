import java.util.HashSet;

public class ServerObject {
  private String value;
  private boolean readLock;
  private boolean writeLock;
  protected HashSet<String> readLockOwner;
  protected String writeLockOwner;

  ServerObject(String value) {
    this.value = value;
    this.readLock = false;
    this.writeLock = false;
    this.readLockOwner = new HashSet<>();
    this.writeLockOwner = null;
  }

  public String getValue() {
    return this.value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public boolean getReadLock() {
    return this.readLock;
  }

  public void setReadLock(boolean flag) {
    this.readLock = flag;
  }

  public boolean getWriteLock() {
    return this.writeLock;
  }

  public void setWriteLock(boolean flag) {
    this.writeLock = flag;
  }
}
