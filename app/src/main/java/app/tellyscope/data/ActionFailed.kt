// SPDX-License-Identifier: GPL-3.0-only
package app.tellyscope.data

/** A change the TV refused. [message] is one plain line for the snackbar; [details] is what the TV printed, for a problem report. */
class ActionFailed(message: String, val details: String) : IllegalStateException(message)
