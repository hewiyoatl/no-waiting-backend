package actors

import play.api.inject.SimpleModule
import play.api.inject._
import tasks.CodeBlockTask

class TasksReservationUpdates extends SimpleModule(bind[CodeBlockTask].toSelf.eagerly())

