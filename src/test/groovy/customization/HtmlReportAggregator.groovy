package customization

import com.athaydes.spockframework.report.internal.AbstractHtmlCreator
import com.athaydes.spockframework.report.internal.SpecData
import com.athaydes.spockframework.report.internal.SpecSummaryNameOption
import com.athaydes.spockframework.report.internal.StringFormatHelper
import com.athaydes.spockframework.report.util.Utils
import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder
import spock.lang.Title

import static com.athaydes.spockframework.report.internal.ReportDataAggregator.getAllAggregatedDataAndPersistLocalData
import static com.athaydes.spockframework.report.internal.SpecSummaryNameOption.CLASS_NAME
import static com.athaydes.spockframework.report.internal.SpecSummaryNameOption.CLASS_NAME_AND_TITLE
import static com.athaydes.spockframework.report.internal.SpecSummaryNameOption.CLASS_NAME_AND_TITLE
import static com.athaydes.spockframework.report.internal.SpecSummaryNameOption.TITLE

@Slf4j
class HtmlReportAggregator  extends AbstractHtmlCreator<Map> {
    final Map<String, Map> aggregatedData = [ : ]

    def stringFormatter = new StringFormatHelper()

    String projectName
    String projectVersion
    String aggregatedJsonReportDir
    SpecSummaryNameOption specSummaryNameOption = CLASS_NAME_AND_TITLE

    protected HtmlReportAggregator() {
        // provided for testing only (need to Mock it)
    }

    @Override
    String cssDefaultName() { 'summary-report.css' }

    void aggregateReport(SpecData data, Map stats ) {
        def specName = Utils.getSpecClassName( data )
        def allFeatures = data.info.allFeaturesInExecutionOrder.groupBy { feature -> Utils.isSkipped( feature ) }

        def specTitle = Utils.specAnnotation( data, Title )?.value() ?: ''
        def narrative = data.info.narrative ?: ''

        aggregatedData[ specName ] = Utils.createAggregatedData(
                allFeatures[ false ], allFeatures[ true ], stats, specTitle, narrative )
    }

    void writeOut() {
        final reportsDir = outputDirectory as File // try to force it into being a File!
        if ( existsOrCanCreate( reportsDir ) ) {
            final aggregatedReport = new File( reportsDir, 'index.html' )
            final jsonDir = aggregatedJsonReportDir ? new File( aggregatedJsonReportDir ) : reportsDir

            try {
                def allData = getAllAggregatedDataAndPersistLocalData( jsonDir, aggregatedData )
                aggregatedData.clear()
                aggregatedReport.write( reportFor( allData ), 'UTF-8' )
            } catch ( e ) {
                log.warn( "Failed to create aggregated report", e )
            }
        } else {
            log.warn "Cannot create output directory: {}", reportsDir?.absolutePath
        }
    }

    static boolean existsOrCanCreate( File reportsDir ) {
        reportsDir?.exists() || reportsDir?.mkdirs()
    }

    @Override
    protected String reportHeader( Map data ) {
        'Custom specification run results'
    }

    @Override
    protected void writeSummary(MarkupBuilder builder, Map json ) {
        def stats = Utils.aggregateStats( json )
        def cssClassIfTrue = { isTrue, String cssClass ->
            if ( isTrue ) [ 'class': cssClass ] else Collections.emptyMap()
        }

        if ( projectName && projectVersion ) {
            builder.div( 'class': 'project-header' ) {
                span( 'class': 'project-name', "Project: ${projectName}" )
                span( 'class': 'project-version', "Version: ${projectVersion}" )
            }
        }

        builder.div( 'class': 'summary-report' ) {
            h3 'Specifications summary:'
            builder.div( 'class': 'date-test-ran', whenAndWho.whenAndWhoRanTest( stringFormatter ) )
            table( 'class': 'summary-table' ) {
                thead {
                    th 'Total classes'
                    th 'Passed features'
                    th 'Failed features'
                    th 'Feature success rate'
                    th 'Total time'
                }
                tbody {
                    tr {
                        td stats.total
                        td stats.fTotal - (stats.fFails + stats.fErrors)
                        td( cssClassIfTrue( stats.fFails+stats.fErrors, 'failure' ), stats.fFails+stats.fErrors )
                        td stringFormatter.toPercentage( Utils.successRate( stats.fTotal, stats.fFails+stats.fErrors ) )
                        td stringFormatter.toTimeDuration( stats.time )
                    }
                }
            }
        }
    }

    @Override
    protected void writeDetails( MarkupBuilder builder, Map data ) {
        builder.h3 'Specifications:'
        builder.table( 'class': 'summary-table' ) {
            thead {
                th 'Name'
                th 'Features'
                th 'Failed'
                th 'Skipped'
                th 'Success rate'
                th 'Time'
            }
            tbody {
                data.keySet().sort().each { String specName ->
                    def stats = data[ specName ].stats
                    def title = data[ specName ].title
                    writeSpecSummary( builder, stats, specName, title )
                }
            }
        }

    }

    protected void writeSpecSummary( MarkupBuilder builder, Map stats, String specName, String title ) {
        def cssClasses = [ ]
        if ( stats.totalRuns == 0 ) {
            cssClasses << 'ignored'
        } else {
            if ( stats.failures || stats.errors ) cssClasses << 'failure'
        }
        builder.tr( cssClasses ? [ 'class': cssClasses.join( ' ' ) ] : null ) {
            td {
                switch ( specSummaryNameOption ) {
                    case CLASS_NAME_AND_TITLE:
                        a( href: "${specName}.html", specName )
                        if ( title ) {
                            div( 'class': 'spec-title', title )
                        }
                        break
                    case CLASS_NAME:
                        a( href: "${specName}.html", specName )
                        break
                    case TITLE:
                        if ( title ) {
                            a( href: "${specName}.html" ) {
                                div( 'class': 'spec-title', title )
                            }
                        } else {
                            a( href: "${specName}.html", specName )
                        }
                        break
                }
            }
            td stats.totalRuns
            td stats.failures + stats.errors
            td stats.skipped
            td stringFormatter.toPercentage( stats.successRate )
            td stringFormatter.toTimeDuration( stats.time )
        }
    }
}

