public class ServerObject {
  private String value;
  private boolean readLock;
  private boolean writeLock;

  ServerObject(String value) {
    this.value = value;
    this.readLock = false;
    this.writeLock = false;
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
