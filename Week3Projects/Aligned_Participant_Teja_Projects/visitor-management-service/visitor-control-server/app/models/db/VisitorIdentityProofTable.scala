package models.db

import models.VisitorIdentityProof
import slick.jdbc.MySQLProfile.api._

class VisitorIdentityProofTable(tag: Tag) extends Table[VisitorIdentityProof](tag, "visitor_identity_proof") {
  def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
  def visitorId = column[Long]("visitor_id")
  def identityProof = column[Array[Byte]]("identity_proof")
  def * = (id, visitorId, identityProof) <> ((VisitorIdentityProof.apply _).tupled, VisitorIdentityProof.unapply)
}
