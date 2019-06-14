package gov.wicourts.cc.reports.snippet

import net.liftweb.util.CssSel
import net.liftweb.util.Helpers._
import org.joda.time.LocalDate
import org.joda.time.LocalDateTime
import scala.xml.NodeSeq
import scalaz.syntax.std.option.ToOptionIdOps

import gov.wicourts.cc.model.CompleteParty
import gov.wicourts.webcommon.snippet._
import gov.wicourts.cc.reports.snippet.RmcReport.FormStatutes
import gov.wicourts.common.syntax.dates._

case class Gf121Args(
  caption: Option[String],
  caseNo: String,
  dateOfJudgment: LocalDate,
  debtor: List[CompleteParty],
  currDateTime: LocalDateTime
)

object Gf121 extends SimpleReportGenerator[Gf121Args]{

  override val reportId: String = "gf121"
  override val reportDesc: String = "Certificate of Satisfaction of Judgment"
  override val cssFiles: List[String] = {
    StandardReportHeader.cssFiles :::
      RmcReportFooter.cssFiles :::
      super.cssFiles
  }

  override def render[RType <: ReportType](
    a: Gf121Args,
    reportType: RType
  ): Option[RType#RetType] = {
    val footer = RmcReportFooter.render(
      report = this,
      meeting = RmcReport.RmcMeeting._200005,
      formStatutes = FormStatutes("806.22").some
    )
    val generatable = ReportGeneratableImpl(
      reportId = reportId,
      cssFiles = cssFiles,
      header = NodeSeq.Empty,
      subHeader = NodeSeq.Empty,
      content = cssSel(a).apply(template(reportType)),
      footer = footer
    )
    ReportGenerator.generate(generatable, reportType)
  }

  private def cssSel(args: Gf121Args): CssSel = {
    ".dateOfJudgment *" #> args.dateOfJudgment.inSimpleDateFormat &
    ".currDateTime *" #> args.currDateTime.inRfc822Format // &
//    ".debtor *" #> args.debtor(partyCss(_))
  }

//  private def partyCss(party: CompleteParty): CssSel = {
//    ".debtorFullName *" #> party.party.fullNameFirstNameFirst &
//    ".debtorPrimAddr *" #> party.partyAddr.flatMap(_.primAddr) &
//    ".debtorSecAddr *" #> party.partyAddr.flatMap(_.secAddr) &
//    ".debtorCityStZip *" #> party.partyAddr.map(_.addr.cityStateZip)
//  }

}
