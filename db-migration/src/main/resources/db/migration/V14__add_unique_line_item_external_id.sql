ALTER TABLE line_items
    ADD CONSTRAINT line_items_external_line_item_id_key UNIQUE (external_line_item_id);
