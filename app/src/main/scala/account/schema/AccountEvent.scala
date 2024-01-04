package account.schema

import caliban.schema.Schema
import caliban.schema.Annotations.GQLDescription


@GQLDescription("Notifies the branch manager of updates")
enum AccountEvent:
  case DEBIT, CREDIT
end AccountEvent
