package com.itechart.project.routes.access

import com.itechart.project.domain.user.Role

object AccessChecker {

  def isResourceAvailable(userRole: Role, availableRoles: List[Role]): Boolean = availableRoles.contains(userRole)

}
