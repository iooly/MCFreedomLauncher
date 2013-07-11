package com.google.gson;

public final class JsonSyntaxException extends JsonParseException
{
  public JsonSyntaxException(String msg)
  {
    super(msg);
  }

  public JsonSyntaxException(String msg, Throwable cause) {
    super(msg, cause);
  }

  public JsonSyntaxException(Throwable cause)
  {
    super(cause);
  }
}