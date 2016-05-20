// Copyright (c) Philipp Wagner. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package elastic.client;

import elastic.client.bulk.configuration.BulkProcessorConfiguration;
import elastic.mapping.ElasticSearchMapping;
import elastic.utils.ElasticSearchUtils;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Client;
import utils.JsonUtilities;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public class ElasticSearchClient<TEntity> implements AutoCloseable {

    private final Client client;
    private final String indexName;
    private final ElasticSearchMapping mapping;
    private final BulkProcessor bulkProcessor;

    public ElasticSearchClient(final Client client, final String indexName, final ElasticSearchMapping mapping, final BulkProcessorConfiguration bulkProcessorConfiguration) {
        this.client = client;
        this.indexName = indexName;
        this.mapping = mapping;
        this.bulkProcessor = bulkProcessorConfiguration.build(client);
    }

    public void createIndex() {
        if(!ElasticSearchUtils.indexExist(client, indexName).isExists()) {
            ElasticSearchUtils.createIndex(client, indexName);
        }
    }

    public void createMapping() {
        if(ElasticSearchUtils.indexExist(client, indexName).isExists()) {
            ElasticSearchUtils.putMapping(client, indexName, mapping);
        }
    }

    public void index(TEntity entity) {
        index(Arrays.asList(entity));

        bulkProcessor.flush();
    }

    public void index(List<TEntity> entities) {
        index(entities.stream());
    }

    public void index(Stream<TEntity> entities) {
        entities
                .map(x -> JsonUtilities.convertJsonToBytes(x))
                .filter(x -> x.isPresent())
                .map(x -> createIndexRequest(x.get()))
                .forEach(bulkProcessor::add);
    }

    private IndexRequest createIndexRequest(byte[] messageBytes) {
        return client.prepareIndex()
                .setId(UUID.randomUUID().toString())
                .setIndex(indexName)
                .setType(mapping.getIndexType())
                .setSource(messageBytes)
                .request();
    }

    @Override
    public void close() throws Exception {
        // If we ever need to close opened resources, it would go here ...
    }
}