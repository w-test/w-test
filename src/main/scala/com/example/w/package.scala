package com.example

/**
  * Created by Diem on 11.07.2017.
  */
package object w {
  def using[T <: { def close() }](resource: T)(block: T => Unit) {
    try {
      block(resource)
    } finally {
      if (resource != null) resource.close()
    }
  }
}
