## or
			Source s_backup = s.clone();
			try{
				//A
			}catch (Exception e){
				s.revert(s_backup);
				//B
			}

## many
			Source s_backup = s.clone();
			try {
				while(true){
					s_backup = s.clone();
					//B
				}
			}catch (Exception e){
				s.revert(s_backup);
			}	
			
			
## ‚È‚­‚Ä‚à‚æ‚¢
			Source s_backup = s.clone();
			try{
				//A
			}catch (Exception e){
				s.revert(s_backup);
			}
			
## Check			
		public Expr check(Check_status cs) throws Exception{

		}
		
if(v.refinement_type_clause!=null){
					if(v.refinement_type_clause.refinement_type!=null){
						v.refinement_type_clause.refinement_type.assert_refinement(cs, v, v.get_Expr(cs));
					}else if(v.refinement_type_clause.ident!=null){
						Refinement_type rt = cs.get_refinement_type(v.refinement_type_clause.ident);
						if(rt!=null){
							rt.assert_refinement(cs, v, v.get_Expr(cs));
						}
					}
				}
		
			
					