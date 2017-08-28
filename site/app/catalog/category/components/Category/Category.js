import React, {Component} from 'react'
import { connect } from 'react-redux'
import { resolve } from 'core/decorator/reduxResolve'
import queryString from 'query-string'
import { Helmet } from 'react-helmet'
import SearchResults from 'catalog/search/components/SearchResults'
import {SearchCapable} from 'catalog/search/decorator/searchCapable'
import {withScrollOnUpdate} from 'core/components/ScrollToTop'
import {fetchSeoProperties} from 'seo/actions'
import {getCategorySeoProperties} from 'seo/selectors'

const getSearchParams = props => {
    const {match : {params : {category}}} = props
    const {q, page, sort, ...filters} = queryString.parse(props.location.search)
    return {q, page, sort, category, ...filters}
}

@withScrollOnUpdate
@SearchCapable('/api/catalog/search', getSearchParams)
export class Category extends Component {
    static propTypes = {
        isFetching : React.PropTypes.bool, // is currently fetching search data
        location : React.PropTypes.object.isRequired,
        match : React.PropTypes.object,
        metadata : React.PropTypes.object, // search metadata
        results : React.PropTypes.array, // search result list
    }

    componentWillReceiveProps(nextProps) {
        const { match: oldMatch } = this.props
        const { match: newMatch } = nextProps

        if (oldMatch.url !== newMatch.url) {
            nextProps.fetchSeoProperties({ entityType: 'CATEGORY', entityURI: `/${newMatch.params.category}` })
        }
    }

    render() {
        const { isFetching, location, match, metadata, results, seoProperties } = this.props
        return (
            <div>
                <Helmet titleTemplate='Heat Clinic - %s'>
                    {seoProperties.title && (
                        <title>{seoProperties.title}</title>
                    )}
                    {seoProperties.description && (
                        <meta name="description" content={seoProperties.description} />
                    )}
                </Helmet>
                <SearchResults
                            key="search"
                            headerTitle={seoProperties.title}
                            isLoading={isFetching}
                            match={match}
                            location={location}
                            metadata={metadata}
                            results={results}/>
            </div>
        )
    }
}

const mapStateToProps = (state, props) => {
    return {
        seoProperties: getCategorySeoProperties(state, props)
    }
}

const dispatchResolve = (resolver, props) => {
    const params = props.match.params
    resolver.resolve(props.fetchSeoProperties, { entityType: 'CATEGORY', entityURI: `/${params.category}` })
}

export default connect(mapStateToProps, { fetchSeoProperties })(
    resolve(dispatchResolve)(Category)
)
