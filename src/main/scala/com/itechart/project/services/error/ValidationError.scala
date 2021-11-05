package com.itechart.project.services.error

import scala.util.control.NoStackTrace

trait ValidationError extends RuntimeException with NoStackTrace {
  def message: String
}
