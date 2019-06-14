package gov.wicourts.testing.rest

import net.liftweb.common.Loggable
import net.liftweb.util.BasicTypesHelpers.asBoolean
import org.joda.time.LocalDate
import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat
import scala.io.Source
import scala.xml.XML
import scalaz.\/
import scalaz.std.list._
import scalaz.std.option._
import scalaz.syntax.std.option._
import scalaz.syntax.std.string._
import scalaz.syntax.tag.ToTagOps
import scalaz.syntax.traverse.ToTraverseOps

import gov.wicourts.cc.CountyContext
import gov.wicourts.cc.model.Case.ToCaseNoCol
import gov.wicourts.cc.model.CompleteJuror
import gov.wicourts.cc.model.CtofcSignature
import gov.wicourts.cc.model.EFilingForm
import gov.wicourts.cc.model.FeeWaiver.WaiverChoice
import gov.wicourts.cc.model.FullName
import gov.wicourts.cc.model.Noticee
import gov.wicourts.cc.model.NoticeeCase
import gov.wicourts.cc.model.Pool
import gov.wicourts.cc.model.SummonsResponseRequirement
import gov.wicourts.cc.model.WcisClsCode
import gov.wicourts.cc.reports.snippet._
import gov.wicourts.cc.service.model.CalWithDates
import gov.wicourts.cc.service.model.MailNotcServiceType
import gov.wicourts.cc.service.model.NotcServiceType
import gov.wicourts.cc.service.model.SccaCaseCaptions
import gov.wicourts.cc.tx._
import gov.wicourts.common.CaseNo
import gov.wicourts.common.config.AppConfig
import gov.wicourts.common.syntax.option._
import gov.wicourts.review.models.da.CompleteRungDaCitation
import gov.wicourts.rung.server.rest.DaRestEndpoint
import gov.wicourts.testing.CcSamples
import gov.wicourts.webcommon.PdfUtils
import gov.wicourts.webcommon.snippet._

class FormSamples private () extends Loggable {

  private val ccSamples = CcSamples()

  private val isoFormatter =
    DateTimeFormat.forPattern("yyyy-MM-dd")

  private def parseDate(dateString: String): Option[LocalDate] =
    \/.fromTryCatchNonFatal(isoFormatter.parseLocalDate(dateString)).fold(_ => none, _.some)

  private def buildCitnDot[RType <: ReportType](
    reportType: RType, params: Map[String, List[String]]
  ): Option[RType#RetType] = {
    val xml = XML.loadString(Source.fromResource("reporting/citnDot/example.xml").getLines.mkString)
    for {
      parsed <- DaRestEndpoint.processDaCaseNew("", xml)
        .leftMap(e => logger.error(s"$e")).toOption
      (_, rungDaCaseJson, _, _, _) = parsed
      charge <- rungDaCaseJson.charges.headOption
      citationJson <- charge.citations.headOption
      completeCitation = CompleteRungDaCitation(66, citationJson)
      citationDot <- completeCitation.rungDaCitationDot
      dmvCommunityCode = citationDot.dmvCommunityCode.map(
        ccSamples.dmvCommunityCodeSample(citationDot.countyNo, _)
      )
      result <- CitnDotReport.render(
        CitnDotReportArgs(
          county = ccSamples.countySample,
          countyAddr = ccSamples.countyAddrSample,
          dmvCommunityCode = dmvCommunityCode,
          rungDaCitation = completeCitation.rungDaCitation,
          rungDaCitationDot = citationDot,
          isRedacted = params.get("isRedacted").flatMap(_.headOption).exists(asBoolean(_).openOr(false)),
          caseNo = None
        ),
        reportType
      )
    } yield result
  }

  private def buildCv410b[RType <: ReportType](reportType: RType, params: Map[String, List[String]]): Option[RType#RetType] = {
    val waiverChoice = WaiverChoice.create(
      code = params.get("waiverChoice").flatMap(_.headOption).getOrDie("Must provide a waiverChoice"),
      otherDenyCode = params.get("otherCode").getOrElse(Nil),
      partialText = params.get("partial").flatMap(_.headOption),
      dueDate = params.get("date").flatMap(_.headOption).flatMap(parseDate),
      noMeritExplanation = params.get("noMeritExplanation").flatMap(_.headOption),
      otherExplanation = params.get("otherExplanation").flatMap(_.headOption)
    ).getOrDie("Invalid fee waiver choice")
    val report = Cv410b(
      ccSamples.countySample,
      ccSamples.caseNoWithNoSuffixSample.some,
      ccSamples.captionSample.some,
      waiverChoice,
    )
    reportType match {
      case ReportType.PDF =>
        report.toPdf.asInstanceOf[RType#RetType].some
      case ReportType.HTML =>
        ReportGenerator.html(report).asInstanceOf[RType#RetType].some
    }
  }

  def buildGf194[RType <: ReportType](reportType: RType, ctofcType: String, params: Map[String, List[String]]): Option[RType#RetType] = {
    val caseNoSt = ccSamples.caseNoWithNoSuffixSample.expanded
    Gf194.render(
      Gf194Args(
        sendingCounty = ccSamples.countySample,
        receivingCounty = ccSamples.countySample2,
        sendingCtofc = ccSamples.ctofcByType(ctofcType).getOrDie(s"$ctofcType is an invalid ctofc type"),
        signature = ccSamples.ctofcSignature,
        caseNo = ccSamples.caseNoWithNoSuffixSample,
        caption = ccSamples.captionSample.some,
        signerName = params.get("signerName").flatMap(_.headOption).getOrDie("Must provide a signerName"),
        signerTitle = params.get("signerTitle").flatMap(_.headOption).getOrDie("Must provide a signerTitle"),
        recipientTitle = params.get("recipientTitle").flatMap(_.headOption).getOrDie("Must provide a recipientTitle"),
        dimds = ccSamples.dimds,
        noticeesForCase = List(
          ccSamples.noticeeCase(ccSamples.countyNoSample, caseNoSt, "First", false, false, false),
          ccSamples.noticeeCase(ccSamples.countyNoSample, caseNoSt, "Second", false, false, true),
          ccSamples.noticeeCase(ccSamples.countyNoSample, caseNoSt, "Third", false, true, false),
          ccSamples.noticeeCase(ccSamples.countyNoSample, caseNoSt, "Fourth", true, false, false)
        ),
        eff = EFilingForm(Gf194.formNumber.unwrap, "", true, false, None, None, None, false, false, "", "", true, false, None)
      ),
      reportType
    )
  }

  def buildCcap250[RType <: ReportType](reportType: RType, params: Map[String, List[String]]): Option[RType#RetType] = {
    val caseNos = params.get("caseNo")
      .map(strList =>
        strList.traverseU(CaseNo.fromString)
          .getOrElse(List(ccSamples.caseNoWithNoSuffixSample))
          .map(_.toCaseNo)
      )
      .getOrElse(List(ccSamples.caseNoWithNoSuffixSample.toCaseNo))
    val sccaCaseNo = params.get("sccaCaseNo")
      .flatMap(_.headOption.map(CaseNo(_)))
      .getOrElse(CaseNo("2018AP000001"))

    val indexFormArgs = IndexFormArgs(
      caption = SccaCaseCaptions.retrieve(sccaCaseNo).fold(
        _ => ccSamples.captionSample,
        _.longCaption.getOrElse(ccSamples.captionSample)
      ),
      cases = caseNos,
      sccaCaseNo = sccaCaseNo,
      rows = Nil
    )

    val _report = Ccap250Report(indexFormArgs)
    reportType match {
      case ReportType.HTML =>
        ReportGenerator.html(_report).toOption.map(_.asInstanceOf[RType#RetType])
      case ReportType.PDF =>
        Some(PdfUtils.concat(1, _report.toPdf).asInstanceOf[reportType.RetType])
    }
  }

  private type NoticeeMap = Map[Noticee#Keyish, NotcServiceType]

  private def makeCalPdf[RType <: ReportType](
    reportType: RType,
    params: Map[String, List[String]],
    report: (NoticeeMap, CalWithDates) => Report,
    copies: Int = 1
  ): Option[RType#RetType] = {
    val calSeqNo = params.get("calSeqNo").flatMap(_.headOption).flatMap(_.parseInt.toOption).getOrElse(0)
    val caseNoSt = params.get("caseNo").flatMap(_.headOption).getOrDie("caseNo is required")
    val caseNo = CaseNo.unapply(caseNoSt).getOrDie("caseNo must be a valid case number")
    val spanish = params.get("spanish").nonEmpty

    val noticees = {
      import gov.wicourts.common.squeryl.CcapSquerylEntryPoint._
      import gov.wicourts.cc.Schema._
      from(table[NoticeeCase])(n =>
        where(n.countyNo === CountyContext.countyNo and n.caseNo === caseNo.expanded)
        select(n)
      )
        .toDbRead(_.toList)
        .run_!
    }
    val noticeeMap: Map[Noticee#Keyish, NotcServiceType] =
      noticees.map(n => Noticee.keyish(n.countyNo, n.caseNo.some, n.noticeeType, n.noticeeTypeSeqNo, None) -> MailNotcServiceType).toMap

    val _report = report(noticeeMap, ccSamples.calWithDates(CountyContext.countyNo, caseNo, calSeqNo, spanish))

    reportType match {
      case ReportType.HTML =>
        ReportGenerator.html(_report).toOption.map(_.asInstanceOf[RType#RetType])
      case ReportType.PDF =>
        Some(PdfUtils.concat(copies, _report.toPdf).asInstanceOf[reportType.RetType])
    }
  }

  private def buildGf180[RType <: ReportType](
    reportType: RType,
    params: Map[String, List[String]]
  ): Option[RType#RetType] = {
    Gf180.render(
      Gf180Args(
        caption = ccSamples.caseSample.sealedCaption,
        caseNo = ccSamples.caseSample.caseNo,
        classCodeDesc = "Class Code Description".some,
        contactDescr = "Clerk of Circuit Court",
        contactPhone = "608-555-1234",
        eFilingFee = BigDecimal(20),
        isRedacted = params.keySet.contains("isRedacted"),
        optInCode = ccSamples.caseSample.optInCode,
        partyFullName =
          params.get("partyFullName")
            .flatMap(_.headOption)
            .map(nameL => FullName(nameF = None, nameM = None, nameL = nameL, suffix = None))
            .getOrElse(ccSamples.completePartySample.party),
        receivingPartyAddr = ccSamples.partyAddrSample.some,
        sendingCountyName = ccSamples.countySample.countyName
      ),
      reportType
    )
  }

  private def buildGf121[RType <: ReportType](
    reportType: RType
  ): Option[RType#RetType] = {
    Gf121.render(
      Gf121Args(
        caption = ccSamples.caseSample.sealedCaption,
        caseNo = ccSamples.caseSample.caseNo,
        dateOfJudgment = LocalDate.now,
        debtor = List.fill(1)(ccSamples.completePartySample),
        currDateTime = LocalDateTime.now()
      ),
      reportType
    )
  }

  private def buildGf208[RType <: ReportType](
    params: Map[String, List[String]],
    reportType: RType
  ): Option[RType#RetType] = {
    val isENotice = params.get("isENotice").nonEmpty
    val county = ccSamples.countySample
    val kase = ccSamples.caseSample
    val filingUser = ccSamples.party(county.countyNo, kase.caseNo, "Filer")
    val representedParties = List(
      ccSamples.party(county.countyNo, kase.caseNo, "John"),
      ccSamples.party(county.countyNo, kase.caseNo, "Jill"),
      ccSamples.party(county.countyNo, kase.caseNo, "Jane")
    )
    val toParty = ccSamples.party(ccSamples.kenoshaCountyNo, kase.caseNo, "Receiver")
    Gf208.render(
      Gf208Args(
        fromCountyName = county.countyName,
        caseNo = kase.caseNo,
        caseCaption = ccSamples.captionSample.some,
        signingCtofcName = ccSamples.judgeCtofcSample.fullNameFirstNameFirst,
        filingUserName = filingUser.fullNameFirstNameFirst,
        filingUserType = "Attorney",
        partyNames = representedParties.map(_.fullNameFirstNameFirst),
        isENotice = isENotice,
        recipientName = toParty.fullNameFirstNameFirst,
        recipientAddr = ccSamples.partyAddrSample.addr.some
      ),
      reportType
    )
  }

  private def buildSc500e[RType <: ReportType](
    params: Map[String, List[String]],
    reportType: RType
  ): Option[RType#RetType] = {
    val defendantCount = params.get("defendantCount").flatMap(_.flatMap(_.parseInt.toOption).headOption).getOrElse(5)
    val plaintiffCount = params.get("plaintiffCount").flatMap(_.flatMap(_.parseInt.toOption).headOption).getOrElse(2)
    val phone = params.get("phone").flatMap(_.headOption)
    Sc500e.render(
      Sc500eArgs(
        ccSamples.countySample,
        ccSamples.caseNoWithNoSuffixSample.expanded,
        ccSamples.captionSample,
        ccSamples.wcisClsCodeSample,
        amended = true,
        ccSamples.countyAddrSample,
        LocalDateTime.now,
        phone,
        params.getOrElse("summons", Nil).headOption.cata(
          SmallClaimsArgs.parseSummonsResponseRequirement,
          SummonsResponseRequirement.Appear
        ),
        LocalDate.now,
        LocalDate.now.some,
        List.fill(plaintiffCount)(ccSamples.completePartySample),
        List.fill(defendantCount)(ccSamples.completePartySample),
        params
          .getOrElse("lang", Nil)
          .headOption
          .flatMap(ReportLanguage.fromCode)
          .getOrElse(ReportLanguage.English)
      ),
      reportType
    )
  }

  private def buildSc516e[RType <: ReportType](
    params: Map[String, List[String]],
    reportType: RType
  ): Option[RType#RetType] = {
    val defendantCount = params.get("defendantCount").flatMap(_.flatMap(_.parseInt.toOption).headOption).getOrElse(5)
    val plaintiffCount = params.get("plaintiffCount").flatMap(_.flatMap(_.parseInt.toOption).headOption).getOrElse(2)
    Sc516e.render(
      Sc516eArgs(
        county = ccSamples.countySample,
        caseNo = ccSamples.caseNoWithNoSuffixSample.expanded,
        caseCaption = ccSamples.captionSample,
        wcisClsCode = WcisClsCode("SC", WcisClsCode.eviction, "Test", true, false),
        amended = true,
        ccJudgeOrCommissioner = ccSamples.judgeCtofcSample,
        countyAddr = ccSamples.countyAddrSample,
        appearanceDateTime = LocalDateTime.now,
        plaintiffs = List.fill(plaintiffCount)(ccSamples.completePartySample),
        defendants = List.fill(defendantCount)(ccSamples.completePartySample),
        reportLanguage = params
          .getOrElse("lang", Nil)
          .headOption
          .flatMap(ReportLanguage.fromCode)
          .getOrElse(ReportLanguage.English)
      ),
      reportType
    )
  }

  def lookup[RType <: ReportType](formNo: String, ctofcType: String, params: Map[String, List[String]], reportType: RType): Option[RType#RetType] = {
    formNo match {
      case CitnDotReport.reportId =>
        buildCitnDot(reportType, params)
      case Cv410b.reportId =>
        buildCv410b(reportType, params)
      case Cv408Report.reportId =>
        val sStatus = params.get("signStatus").flatMap(_.headOption).getOrDie("signStatus required")
        makeCalPdf(
          reportType,
          params,
          (nm, cwd) => Cv408Report(
            new Cv408ReportParams(
              false,
              true,
              true,
              true,
              params.get("otherText").flatMap(_.headOption),
              true,
              sStatus,
              nm,
              Some(
                ccSamples.ctofc(ctofcType.some),
                CtofcSignature(-1, "-1234", "Super Signature", "Super title", true, true).some
              ),
              List(cwd)
            ),
            List(cwd)
          ),
          4
        )
      case Cv980Report.reportId =>
        makeCalPdf(
          reportType,
          params,
          (_, cwd) => Cv980Report(List(cwd)),
          4
        )
      case Gf101Report.reportId =>
        makeCalPdf(
          reportType,
          params,
          (nm, cwd) => {
            val rs = RecordStore(List(cwd))
            rs.selectAll()
            Gf101Report.apply(rs, new NoticeOfHearingReportParams(nm, true, List(cwd)), List(cwd))
          }
        )
      case Gf101SReport.reportId =>
        makeCalPdf(
          reportType,
          params,
          (nm, cwd) => {
            val rs = RecordStore(List(cwd))
            rs.selectAll()
            Gf101SReport.apply(rs, new NoticeOfHearingReportParams(nm, true, List(cwd)), List(cwd))
          }
        )
      case Gf130Report.reportId =>
        makeCalPdf(
          reportType,
          params,
          (_, cwd) => Gf130Report.apply(false, List(cwd))
        )
      case Gf133AReport.reportId =>
        val pool: Pool = ccSamples.pool(CountyContext.countyNo)
        val jurors: List[CompleteJuror] = ccSamples.jurors(CountyContext.countyNo)
        reportType match {
          case ReportType.HTML => None
          case ReportType.PDF => Gf133AReport.apply(pool, jurors).toPdf.asInstanceOf[reportType.RetType].some
        }
      case Gf194.reportId =>
        buildGf194(reportType, ctofcType, params)
      case Gf180.reportId =>
        buildGf180(reportType, params)
      case Gf121.reportId =>
        buildGf121(reportType)
      case Gf208.reportId =>
        buildGf208(params, reportType)
      case Sc500e.reportId =>
        buildSc500e(params, reportType)
      case Sc516e.reportId =>
        buildSc516e(params, reportType)
      case Gn3500Report.reportId =>
        makeCalPdf(
          reportType,
          params,
          (nm, cwd) => Gn3500Report(new Gn3500ReportParams(false, nm, AccountPeriod.Annual, List(cwd)), List(cwd)),
          4
        )
      case Gn3530Report.reportId =>
        makeCalPdf(
          reportType,
          params,
          (nm, cwd) => Gn3530Report(
            new Gn3530ReportParams(
              nm,
              true,
              List(cwd)
            ),
            List(cwd)
          ),
          4
        )
      case Gn3540Report.reportId =>
        val sStatus = params.get("signStatus").flatMap(_.headOption).getOrDie("signStatus required")
        makeCalPdf(
          reportType,
          params,
          (nm, cwd) => Gn3540Report(
            new Gn3540ReportParams(
              false,
              true,
              true,
              "2018",
              true,
              "2017",
              LocalDate.now.some,
              true,
              sStatus,
              nm,
              true,
              Some(
                ccSamples.ctofc(ctofcType.some),
                CtofcSignature(-1, "-1234", "Super Signature", "Super title", true, true).some
              ),
              List(cwd)
            ),
            List(cwd)
          ),
          4
        )
      case Jc1633Report.reportId =>
        makeCalPdf(
          reportType,
          params,
          (nm, cwd) => Jc1633Report(
            new Jc1633ReportParams(
              nm,
              true,
              params.get("signStatus").flatMap(_.headOption).getOrDie("signStatus required"),
              false,
              Some(
                ccSamples.ctofc(ctofcType.some),
                CtofcSignature(-1, "-1234", "Super Signature", "Super title", true, true).some
              ),
              List(cwd)
            ),
            List(cwd)
          ),
          4
        )
      case Jd1700Report.reportId =>
        val worker = params.get("worker").flatMap(_.headOption).getOrDie("worker required")
        makeCalPdf(
          reportType,
          params,
          (nm, cwd) => {
            val rs = RecordStore(List(cwd))
            rs.selectAll()
            Jd1700Report(
              rs,
              new Jd1700ReportParams(nm, true, worker, List(cwd)),
              List(cwd)
            )
          },
          4
        )
      case Jd1700SReport.reportId =>
        val worker = params.get("worker").flatMap(_.headOption).getOrDie("worker required")
        makeCalPdf(
          reportType,
          params,
          (nm, cwd) => {
            val rs = RecordStore(List(cwd))
            rs.selectAll()
            Jd1700SReport(
              rs,
              new Jd1700ReportParams(nm, true, worker, List(cwd)),
              List(cwd)
            )
          },
          4
        )
      case Jd1709Report.reportId =>
        makeCalPdf(
          reportType,
          params,
          (_, cwd) => Jd1709Report(false, List(cwd))
        )
      case Jd1720Report.reportId =>
        val sStatus = params.get("signStatus").flatMap(_.headOption).getOrDie("signStatus required")
        makeCalPdf(
          reportType,
          params,
          (nm, cwd) => {
            Jd1720Report.apply(new Jd1720ReportParams(nm, true, sStatus, false, Some(
              ccSamples.ctofc(ctofcType.some),
              CtofcSignature(-1, "-1234", "Super Signature", "Super title", true, true).some
            ), List(cwd)), List(cwd))
          },
          params.get("copies").flatMap(_.headOption.flatMap(_.parseInt.toOption)).getOrElse(1)
        )
      case Jd1724Report.reportId =>
        makeCalPdf(
          reportType,
          params,
          (nm, cwd) => {
            val rs = RecordStore(List(cwd))
            rs.selectAll()
            Jd1724Report.apply(rs, new Jd1724ReportParams(nm, true, false, false, List(cwd)), List(cwd))
          }
        )
      case Jd1765Report.reportId =>
        val sStatus = params.get("signStatus").flatMap(_.headOption).getOrDie("signStatus required")
        makeCalPdf(
          reportType,
          params,
          (nm, cwd) => {
            Jd1765Report.apply(new Jd1765ReportParams(
              true,
              LocalDate.now.some,
              false,
              true,
              false,
              true,
              false,
              true,
              true,
              false,
              true,
              true,
              sStatus,
              nm,
              true,
              Some(
                ccSamples.ctofc(ctofcType.some),
                CtofcSignature(-1, "-1234", "Super Signature", "Super title", true, true).some
              ),
              List(cwd)
            ), List(cwd))
          },
          params.get("copies").flatMap(_.headOption.flatMap(_.parseInt.toOption)).getOrElse(1)
        )
      case Pr101Report.reportId =>
        makeCalPdf(
          reportType,
          params,
          (nm, cwd) => {
            val rs = RecordStore(List(cwd))
            rs.selectAll()
            Pr101Report.apply(rs, new NoticeOfHearingReportParams(nm, true, List(cwd)), List(cwd))
          }
        )
      case Pr1824Report.reportId =>
        val sStatus = params.get("signStatus").flatMap(_.headOption).getOrDie("signStatus required")
        makeCalPdf(
          reportType,
          params,
          (nm, cwd) => {
            Pr1824Report.apply(new Pr1824ReportParams(LocalDate.now.some, None, sStatus, nm, true, false, Some(
              ccSamples.ctofc(ctofcType.some),
              CtofcSignature(-1, "-1234", "Super Signature", "Super title", true, true).some
            ),
              List(cwd)
            ), List(cwd))
          }
        )
      case Pr1825Report.reportId =>
        makeCalPdf(
          reportType,
          params,
          (nm, cwd) => Pr1825Report(new Pr1825ReportParams(false, nm, true, false, None, true, List(cwd)), List(cwd))
        )
      case Pr1826Report.reportId =>
        val sStatus = params.get("signStatus").flatMap(_.headOption).getOrDie("signStatus required")
        makeCalPdf(
          reportType,
          params,
          (nm, cwd) => {
            Pr1826Report.apply(
              new Pr1826ReportParams(
                nm,
                true,
                sStatus,
                true,
                Map(cwd.cal.calKey -> Some(LocalDate.now)),
                Some(
                  ccSamples.ctofc(ctofcType.some),
                  CtofcSignature(-1, "-1234", "Super Signature", "Super title", true, true).some
                ),
                List(cwd)
              ),
              List(cwd)
            )
          }
        )
      case Tr302Report.reportId =>
        makeCalPdf(
          reportType,
          params,
          (_, cwd) => Tr302Report.apply(false, List(cwd))
        )
      case Ccap250Report.reportId =>
        buildCcap250(
          reportType,
          params
        )
      case _ => None
    }
  }
}

object FormSamples {
  def apply(): FormSamples =
    AppConfig.developerOrDie(new FormSamples())
}
