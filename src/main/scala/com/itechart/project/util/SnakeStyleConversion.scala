package com.itechart.project.util

object SnakeStyleConversion {
  def camelToSnake(str: String): String = {
    "[A-Z\\d]".r
      .replaceAllIn(str, m => "_" + m.group(0).toLowerCase())
  }

  def snakeToCamel(str: String): String = {
    "_([a-z\\d])".r
      .replaceAllIn(str, _.group(1).toUpperCase())
  }

  def normalizedSnakeCase(str: String): String = {
    val firstChar      = str.charAt(0).toLower
    val remainingChars = str.substring(1)
    val pureCamelCase  = s"$firstChar$remainingChars"

    camelToSnake(pureCamelCase).toUpperCase
  }
}
